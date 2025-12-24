import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CNN Article Storage Server
 * Receives articles from Python and saves to text files
 */
public class CNNArticleServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        
        ArticleStorage storage = new ArticleStorage("articles_data");
        ArticleHandler handler = new ArticleHandler(storage);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/articles", handler);
        server.setExecutor(null);
        server.start();
        
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   CNN Article Storage Server          ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║ Port: " + port + "                          ║");
        System.out.println("║ Storage: Text Files                    ║");
        System.out.println("║ Directory: articles_data/              ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("✅ Server ready!\n");
    }
}


// ============================================================================
// ARTICLE CLASS (Custom Object)
// ============================================================================
class Article {
    String url;
    String title;
    String author;
    String date;
    String content;
    String receivedAt;
    
    public Article(String url, String title, String author, String date, String content) {
        this.url = url;
        this.title = title;
        this.author = author;
        this.date = date;
        this.content = content;
        this.receivedAt = LocalDateTime.now().toString();
    }
    
    public String getId() {
        // Generate unique ID from URL
        return Integer.toHexString(url.hashCode());
    }
    
    public int getWordCount() {
        return content != null ? content.split("\\s+").length : 0;
    }
}


// ============================================================================
// STORAGE (Saves to Text Files)
// ============================================================================
class ArticleStorage {
    private String baseDir;
    private Set<String> savedUrls;
    
    public ArticleStorage(String baseDir) {
        this.baseDir = baseDir;
        this.savedUrls = new HashSet<>();
        
        // Create directory
        new File(baseDir).mkdirs();
        
        // Load existing URLs
        loadExistingUrls();
        
        System.out.println("📁 Storage: " + new File(baseDir).getAbsolutePath());
        System.out.println("📚 Loaded " + savedUrls.size() + " existing articles\n");
    }
    
    private void loadExistingUrls() {
        // Load URLs from existing files
        File dir = new File(baseDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        
        if (files != null) {
            for (File file : files) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("URL: ")) {
                            savedUrls.add(line.substring(5));
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Skip
                }
            }
        }
    }
    
    public boolean exists(String url) {
        return savedUrls.contains(url);
    }
    
    public void save(Article article) throws IOException {
        // Check duplicate
        if (exists(article.url)) {
            throw new IOException("Article already exists");
        }
        
        // Generate filename
        String id = article.getId();
        String safeTitle = sanitizeFilename(article.title);
        String filename = id + "_" + safeTitle + ".txt";
        
        File file = new File(baseDir, filename);
        
        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("=".repeat(80));
            writer.write("\nCNN ARTICLE\n");
            writer.write("=".repeat(80));
            writer.write("\n\n");
            
            writer.write("URL: " + article.url + "\n");
            writer.write("Title: " + article.title + "\n");
            writer.write("Author: " + article.author + "\n");
            writer.write("Date: " + article.date + "\n");
            writer.write("Received: " + article.receivedAt + "\n");
            writer.write("Word Count: " + article.getWordCount() + "\n");
            
            writer.write("\n");
            writer.write("=".repeat(80));
            writer.write("\nCONTENT\n");
            writer.write("=".repeat(80));
            writer.write("\n\n");
            
            writer.write(article.content);
            
            writer.write("\n\n");
            writer.write("=".repeat(80));
            writer.write("\nEND OF ARTICLE\n");
            writer.write("=".repeat(80));
            writer.write("\n");
        }
        
        // Add to set
        savedUrls.add(article.url);
        
        System.out.println("✅ Saved: " + article.title);
        System.out.println("   File: " + filename);
        System.out.println("   Words: " + article.getWordCount());
        System.out.println();
    }
    
    private String sanitizeFilename(String title) {
        if (title == null) return "untitled";
        
        // Remove special characters
        String safe = title.replaceAll("[^a-zA-Z0-9-_ ]", "");
        safe = safe.trim().replaceAll("\\s+", "_");
        
        // Limit length
        if (safe.length() > 50) {
            safe = safe.substring(0, 50);
        }
        
        return safe;
    }
    
    public int getCount() {
        return savedUrls.size();
    }
}


// ============================================================================
// HTTP HANDLER (Receives from Python)
// ============================================================================
class ArticleHandler implements HttpHandler {
    private ArticleStorage storage;
    
    public ArticleHandler(ArticleStorage storage) {
        this.storage = storage;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if (method.equals("POST")) {
            handlePost(exchange);
        } else if (method.equals("GET")) {
            handleGet(exchange);
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }
    
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            // Read JSON from Python
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody())
            );
            
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            
            // Parse JSON (simple manual parsing)
            String jsonStr = json.toString();
            
            String url = extractValue(jsonStr, "url");
            String title = extractValue(jsonStr, "title");
            String author = extractValue(jsonStr, "author");
            String date = extractValue(jsonStr, "date");
            String content = extractValue(jsonStr, "content");
            
            // Create Article object
            Article article = new Article(url, title, author, date, content);
            
            // Save to file
            storage.save(article);
            
            // Send success response
            String response = "{\"message\":\"Article saved\",\"id\":\"" + article.getId() + "\"}";
            sendResponse(exchange, 201, response);
            
        } catch (IOException e) {
            if (e.getMessage().contains("already exists")) {
                sendResponse(exchange, 409, "{\"error\":\"Article already exists\"}");
            } else {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Failed to save\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400, "{\"error\":\"Invalid request\"}");
        }
    }
    
    private void handleGet(HttpExchange exchange) throws IOException {
        // Return simple stats
        String response = "{\"totalArticles\":" + storage.getCount() + "}";
        sendResponse(exchange, 200, response);
    }
    
    private String extractValue(String json, String key) {
        // Simple JSON value extraction
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        
        if (start == -1) return "";
        
        start += searchKey.length();
        int end = start;
        
        // Find closing quote (handle escaped quotes)
        while (end < json.length()) {
            if (json.charAt(end) == '"' && (end == 0 || json.charAt(end - 1) != '\\')) {
                break;
            }
            end++;
        }
        
        String value = json.substring(start, end);
        
        // Unescape
        value = value.replace("\\n", "\n");
        value = value.replace("\\r", "\r");
        value = value.replace("\\t", "\t");
        value = value.replace("\\\"", "\"");
        value = value.replace("\\\\", "\\");
        
        return value;
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) 
            throws IOException {
        // Set headers
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        // Send response
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
