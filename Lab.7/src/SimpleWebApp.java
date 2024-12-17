import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SimpleWebApp {
    // Lista globala care contine procesele
    private static final ArrayList<Process> processList = new ArrayList<>();
    private static String selectedAlgorithm = "SJF";  // Algoritmul selectat implicit

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8084), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/process", new ProcessHandler());
        server.createContext("/results", new ResultsHandler()); // Adăugăm handler-ul pentru /results
        server.setExecutor(null); // Use default executor
        server.start();
        System.out.println("Server started on port 8084");
    }

    // Handler pentru pagina principala cu formularul HTML
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html>" +
                    "<head><title>Planificare Procese</title>" +
                    "<style>" +
                    "body {font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;}" +
                    "h1 {color: #333;}" +
                    "form {background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);}" +
                    "input[type='text'], input[type='number'], select {width: 100%; padding: 10px; margin: 10px 0; border-radius: 4px; border: 1px solid #ddd;}" +
                    "input[type='submit'] {background-color: #4CAF50; color: white; border: none; padding: 10px 20px; cursor: pointer; border-radius: 4px;}" +
                    "input[type='submit']:hover {background-color: #45a049;}" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<h1>Introduceti procesele</h1>" +
                    "<form action='/process' method='post'>" +
                    "<label for='processName'>Numele procesului:</label><br>" +
                    "<input type='text' name='processName' required><br>" +
                    "<label for='arrivalTime'>Momentul de sosire (in secunde):</label><br>" +
                    "<input type='number' name='arrivalTime' required><br>" +
                    "<label for='executionTime'>Durata de executie (in secunde):</label><br>" +
                    "<input type='number' name='executionTime' required><br>" +
                    "<label for='algorithm'>Alege algoritmul:</label><br>" +
                    "<select name='algorithm'>" +
                    "<option value='SJF'>SJF (Cea mai scurtă sarcină este prima)</option>" +
                    "<option value='RR'>Round Robin</option>" +
                    "</select><br>" +
                    "<input type='submit' value='Trimite'>" +
                    "</form>" +
                    "<br><a href='/results'>Vezi procesele adaugate</a>" +  // Link pentru a vizualiza procesele
                    "</body>" +
                    "</html>";

            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Handler pentru procesarea datelor trimise prin formular
    static class ProcessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Citim datele din formular (POST)
            String query = new String(exchange.getRequestBody().readAllBytes());
            String[] data = query.split("&");

            // Extragem valorile din formular
            String processName = data[0].split("=")[1];
            int arrivalTime = Integer.parseInt(data[1].split("=")[1]);
            int executionTime = Integer.parseInt(data[2].split("=")[1]);
            String algorithm = data[3].split("=")[1];

            // Setăm algoritmul selectat
            selectedAlgorithm = algorithm;

            // Creăm un obiect pentru proces
            Process process = new Process(processName, arrivalTime, executionTime);

            // Adăugăm procesul în lista globală
            processList.add(process);

            // Sortăm procesele în funcție de algoritmul selectat
            if (selectedAlgorithm.equals("SJF")) {
                // Sortare SJF
                Collections.sort(processList, new Comparator<Process>() {
                    @Override
                    public int compare(Process p1, Process p2) {
                        return Integer.compare(p1.executionTime, p2.executionTime); // Sortare după execuție
                    }
                });
            } else if (selectedAlgorithm.equals("RR")) {
                // Round Robin
                // Nu este necesară o sortare specială pentru Round Robin.
            }

            // Redirecționăm către pagina principală după ce am adăugat procesul
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);  // Redirecționare
            exchange.getResponseBody().close();
        }
    }

    // Handler pentru vizualizarea proceselor adăugate
    static class ResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder executionOrder = new StringBuilder();
            int currentTime = 0;
            int totalTurnaroundTime = 0;
            int totalWaitingTime = 0;

            if (selectedAlgorithm.equals("SJF")) {
                // Calculăm ordinea de execuție și timpii de realizare pentru SJF
                for (Process p : processList) {
                    // Timpul de realizare
                    p.completionTime = currentTime + p.executionTime;

                    // Timpul de așteptare
                    p.turnaroundTime = p.completionTime - p.arrivalTime;

                    // Timpul de așteptare
                    p.waitingTime = p.turnaroundTime - p.executionTime;

                    // Actualizăm totalurile
                    totalTurnaroundTime += p.turnaroundTime;
                    totalWaitingTime += p.waitingTime;

                    executionOrder.append("Procesul ").append(p.name)
                            .append(" incepe la secunda ").append(currentTime)
                            .append(" si se termina la secunda ").append(p.completionTime)
                            .append(".<br>Turnaround Time: ").append(p.turnaroundTime)
                            .append(" secunde.<br>Waiting Time: ").append(p.waitingTime)
                            .append(" secunde.<br><br>");

                    currentTime += p.executionTime;
                }
            } else if (selectedAlgorithm.equals("RR")) {
                // Round Robin
                int quantum = 2; // Exemplu de quantum de timp, se poate ajusta
                ArrayList<Process> rrQueue = new ArrayList<>(processList);
                currentTime = 0;
                while (!rrQueue.isEmpty()) {
                    Process p = rrQueue.remove(0);
                    if (p.executionTime > quantum) {
                        p.executionTime -= quantum;
                        rrQueue.add(p);
                        executionOrder.append("Procesul ").append(p.name)
                                .append(" incepe la secunda ").append(currentTime)
                                .append(" si se va termina la secunda ").append(currentTime + quantum)
                                .append("<br>");
                        currentTime += quantum;
                    } else {
                        executionOrder.append("Procesul ").append(p.name)
                                .append(" incepe la secunda ").append(currentTime)
                                .append(" si se termina la secunda ").append(currentTime + p.executionTime)
                                .append("<br>");
                        currentTime += p.executionTime;
                    }
                }
            }

            // Calculăm timpii medii
            double averageTurnaroundTime = (double) totalTurnaroundTime / processList.size();
            double averageWaitingTime = (double) totalWaitingTime / processList.size();

            // Generăm un răspuns HTML cu rezultatele
            String response = "<html>" +
                    "<head><title>Rezultate Planificare Procese</title>" +
                    "<style>" +
                    "body {font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;}" +
                    "h1 {color: #333;}" +
                    "p {font-size: 18px;}" +
                    "a {color: #4CAF50; text-decoration: none;}" +
                    "a:hover {text-decoration: underline;}" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<h1>Rezultatele planificarii proceselor</h1>" +
                    "<p>Ordinea de executie a proceselor:</p>" +
                    "<p>" + executionOrder.toString() + "</p>" +
                    "<p>Time Media de Turnaround: " + averageTurnaroundTime + " secunde</p>" +
                    "<p>Time Media de Waiting: " + averageWaitingTime + " secunde</p>" +
                    "<a href='/'>Inapoi la formular</a>" +
                    "</body>" +
                    "</html>";

            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Clasa pentru un proces
    static class Process {
        String name;
        int arrivalTime;
        int executionTime;
        int completionTime;  // Timpul de realizare
        int turnaroundTime;  // Timpul de realizare - sosire
        int waitingTime;     // Timpul de așteptare

        Process(String name, int arrivalTime, int executionTime) {
            this.name = name;
            this.arrivalTime = arrivalTime;
            this.executionTime = executionTime;
        }
    }
}
