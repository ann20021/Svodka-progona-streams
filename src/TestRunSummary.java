import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TestRunSummary {

    public enum Status {
        PASSED, FAILED, SKIPPED
    }

    public record TestResult(String name, Status status, long durationMs) {
    }

    public void printSummary(List<TestResult> run) {
        // Используем EnumMap, чтобы статусы шли в строгом, но естественном порядке: PASSED, FAILED, SKIPPED.
        Map<Status, List<String>> byStatus = run.stream()
                .collect(Collectors.groupingBy(
                        TestResult::status,
                        () -> new EnumMap<>(Status.class),
                        Collectors.mapping(TestResult::name, Collectors.toList()) //оставляем только имя
                ));
        //Итог: словарь «Статус → Список имён».

        // Сортируем по убыванию длительности
        List<String> top3Slowest = run.stream()
                .sorted(Comparator.comparingLong(TestResult::durationMs).reversed())
                .limit(3)
                .map(TestResult::name)
                .toList();

        //Фильтруем только PASSED и считаем среднее время.
        double avgPassedDuration = run.stream()
                .filter(r -> r.status() == Status.PASSED)
                .collect(Collectors.averagingLong(TestResult::durationMs));



        // Формируем Отчет
        System.out.println("=== Сводка прогона ===");

        //раз нельзя циклы, возьмем forEach
        List.of(Status.values()).forEach(status -> {
            List<String> names = byStatus.getOrDefault(status, List.of());
            //если вдруг ни 1 тест не упал, чтобы вернулся пустой список, а не null
            System.out.printf("%s (%d): %s%n",
                    status,
                    names.size(),
                    String.join(", ", names));
        });

        System.out.println("Топ-3 долгих: " + String.join(", ", top3Slowest));
        // Locale.US поставит точку (382.5), а не запятую (382,5) в дробном числе
        System.out.printf(Locale.US, "Среднее время прошедших: %.1f ms%n", avgPassedDuration);
    }

    public static void main(String[] args) {
        List<TestResult> run = List.of(
                new TestResult("LoginTest", Status.PASSED, 120),
                new TestResult("CreateTaskTest", Status.PASSED, 940),
                new TestResult("UpdateStatusTest", Status.FAILED, 2100),
                new TestResult("CommentTest", Status.PASSED, 380),
                new TestResult("DeleteTaskTest", Status.FAILED, 760),
                new TestResult("StatsTest", Status.SKIPPED, 0),
                new TestResult("LogoutTest", Status.PASSED, 90)
        );

        new TestRunSummary().printSummary(run);
        
    }
}