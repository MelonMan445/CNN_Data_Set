import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

// Main Article Data Model
class Article {
    private String url;
    private String title;
    private String author;
    private LocalDate publishDate;
    private LocalDateTime receivedDate;
    private int wordCount;
    private String content;
    
    public Article(String url, String title, String author, LocalDate publishDate, 
                   LocalDateTime receivedDate, int wordCount, String content) {
        this.url = url;
        this.title = title;
        this.author = author;
        this.publishDate = publishDate;
        this.receivedDate = receivedDate;
        this.wordCount = wordCount;
        this.content = content;
    }
    
    // Getters
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public LocalDate getPublishDate() { return publishDate; }
    public LocalDateTime getReceivedDate() { return receivedDate; }
    public int getWordCount() { return wordCount; }
    public String getContent() { return content; }
    
    // Analysis methods for visualization
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }
    
    public int getSentenceCount() {
        if (content == null) return 0;
        String[] sentences = content.split("[.!?]+");
        return sentences.length;
    }
    
    public double getAvgWordsPerSentence() {
        int sentences = getSentenceCount();
        return sentences > 0 ? (double) wordCount / sentences : 0;
    }
    
    public String getCategory() {
        // Extract category from URL
        Pattern p = Pattern.compile("cnn\\.com/\\d{4}/\\d{2}/\\d{2}/([^/]+)/");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : "unknown";
    }
    
    @Override
    public String toString() {
        return String.format("Article[title='%s', author='%s', date=%s, words=%d]",
                           title, author, publishDate, wordCount);
    }
}

// Article Collection for Analysis
class ArticleCollection {
    private List<Article> articles;
    
    public ArticleCollection() {
        this.articles = new ArrayList<>();
    }
    
    public void addArticle(Article article) {
        articles.add(article);
    }
    
    public List<Article> getArticles() {
        return new ArrayList<>(articles);
    }
    
    public int getTotalArticles() {
        return articles.size();
    }
    
    // Analytics methods for visualization
    public Map<String, Integer> getArticlesByAuthor() {
        Map<String, Integer> authorCounts = new HashMap<>();
        for (Article a : articles) {
            authorCounts.merge(a.getAuthor(), 1, Integer::sum);
        }
        return authorCounts;
    }
    
    public Map<String, Integer> getArticlesByCategory() {
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (Article a : articles) {
            categoryCounts.merge(a.getCategory(), 1, Integer::sum);
        }
        return categoryCounts;
    }
    
    public Map<LocalDate, Integer> getArticlesByDate() {
        Map<LocalDate, Integer> dateCounts = new HashMap<>();
        for (Article a : articles) {
            dateCounts.merge(a.getPublishDate(), 1, Integer::sum);
        }
        return dateCounts;
    }
    
    public Map<String, Double> getAvgWordCountByCategory() {
        Map<String, List<Integer>> categoryWords = new HashMap<>();
        for (Article a : articles) {
            categoryWords.computeIfAbsent(a.getCategory(), k -> new ArrayList<>())
                        .add(a.getWordCount());
        }
        
        Map<String, Double> avgWords = new HashMap<>();
        categoryWords.forEach((cat, words) -> {
            double avg = words.stream().mapToInt(Integer::intValue).average().orElse(0);
            avgWords.put(cat, avg);
        });
        return avgWords;
    }
    
    public List<Article> getTopArticlesByWordCount(int n) {
        return articles.stream()
                      .sorted(Comparator.comparingInt(Article::getWordCount).reversed())
                      .limit(n)
                      .toList();
    }
}

// Parser for CNN Article Text Files
class CNNArticleParser {
    
    public static Article parseArticleFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        
        // Parse metadata fields
        String url = extractField(content, "URL:");
        String title = extractField(content, "Title:");
        String author = extractField(content, "Author:");
        LocalDate publishDate = parseDate(extractField(content, "Date:"));
        LocalDateTime receivedDate = parseDateTime(extractField(content, "Received:"));
        int wordCount = parseInt(extractField(content, "Word Count:"));
        String articleContent = extractContent(content);
        
        return new Article(url, title, author, publishDate, receivedDate, wordCount, articleContent);
    }
    
    private static String extractField(String content, String fieldName) {
        Pattern p = Pattern.compile(fieldName + "\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1).trim() : "";
    }
    
    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static int parseInt(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static String extractContent(String fileContent) {
        Pattern p = Pattern.compile("CONTENT\\s*=+\\s*(.+?)\\s*=+\\s*END OF ARTICLE", 
                                   Pattern.DOTALL);
        Matcher m = p.matcher(fileContent);
        if (m.find()) {
            return m.group(1).trim()
                    .replaceAll("\\\\u2019", "'")
                    .replaceAll("\\\\u201c", "\"")
                    .replaceAll("\\\\u201d", "\"");
        }
        return "";
    }
    
    public static ArticleCollection parseDirectory(String directoryPath) {
        ArticleCollection collection = new ArticleCollection();
        
        try {
            Files.walk(Paths.get(directoryPath))
                 .filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".txt"))
                 .forEach(path -> {
                     try {
                         Article article = parseArticleFile(path);
                         collection.addArticle(article);
                         System.out.println("Parsed: " + article.getTitle());
                     } catch (Exception e) {
                         System.err.println("Error parsing " + path + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
        }
        
        return collection;
    }
}

// Main class with usage example
public class CNNArticleAnalyzer {
    
    public static void main(String[] args) {
        // Parse all articles from the specified directory
        String articlesDir = "/home/admin/cnn_scape/articles_data";
        ArticleCollection collection = CNNArticleParser.parseDirectory(articlesDir);
        
        
        
        // Display articles by author
        System.out.println("\n--- Articles by Author ---");
        collection.getArticlesByAuthor().entrySet().stream()
                 .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                 .forEach(e -> System.out.printf("%s: %d articles%n", e.getKey(), e.getValue()));
        
        // Display articles by category
        System.out.println("\n--- Articles by Category ---");
        collection.getArticlesByCategory().forEach((cat, count) -> 
            System.out.printf("%s: %d articles%n", cat, count));
        
        // Display average word counts by category
        System.out.println("\n--- Average Word Count by Category ---");
        collection.getAvgWordCountByCategory().forEach((cat, avg) -> 
            System.out.printf("%s: %.0f words%n", cat, avg));
        
        // Top 5 longest articles
        System.out.println("\n--- Top 5 Longest Articles ---");
        collection.getTopArticlesByWordCount(5).forEach(a -> 
            System.out.printf("%s (%d words)%n", a.getTitle(), a.getWordCount()));
        System.out.println("\n=== CNN Article Database Analysis ===");
        System.out.println("Total articles: " + collection.getTotalArticles());
        
        // Export data for visualization (CSV format)
        exportToCSV(collection, "articles_export.csv");
    }
    
    public static void exportToCSV(ArticleCollection collection, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Title,Author,Date,Category,WordCount,SentenceCount,AvgWordsPerSentence");
            
            for (Article a : collection.getArticles()) {
                writer.printf("\"%s\",\"%s\",%s,%s,%d,%d,%.2f%n",
                    a.getTitle().replace("\"", "\"\""),
                    a.getAuthor(),
                    a.getPublishDate(),
                    a.getCategory(),
                    a.getWordCount(),
                    a.getSentenceCount(),
                    a.getAvgWordsPerSentence()
                );
            
            }
            
            System.out.println("\nData exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Error exporting CSV: " + e.getMessage());
        }
    }
}
