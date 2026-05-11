package AI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GymPython {

    private static final String GYM_COMMAND_PROPERTY = "quoridor.gym.command";
    private static final String GYM_COMMAND_ENV = "QUORIDOR_GYM_PYTHON_COMMAND";

    private final GameState state;

    public GymPython(GameState state) {
        this.state = state;
    }

    public int generateMove() {
        return requestMoveFromPython(state).orElseGet(() -> LegalMoveSelector.firstLegalMove(state));
    }

    private OptionalInt requestMoveFromPython(GameState state) {
        String command = configuredGymCommand();
        if (command == null || command.isBlank()) {
            return OptionalInt.empty();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command.trim().split("\\s+"));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(formatState(state));
                writer.newLine();
            }

            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return OptionalInt.empty();
            }

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
            }

            int moveCode = Integer.parseInt(output.trim());
            if (LegalMoveSelector.legalMoves(state).contains(moveCode)) {
                return OptionalInt.of(moveCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            return OptionalInt.empty();
        }

        return OptionalInt.empty();
    }

    private String configuredGymCommand() {
        String command = System.getProperty(GYM_COMMAND_PROPERTY);
        if (command == null || command.isBlank()) {
            command = System.getenv(GYM_COMMAND_ENV);
        }
        return command;
    }

    private String formatState(GameState state) {
        return Arrays.stream(state.getState())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" "));
    }
}
