# CNN Dataset Creator

This project is a **news article dataset generator** built using **Python (scraping)** and **Java (storage & processing)**.  
It automatically collects CNN articles and structures them into a clean dataset for analysis.

---

## Collected Fields

Each article contains:

- URL  
- Publication date  
- Title  
- Author  
- Full article content  

---

## How It Works

- **Python** is responsible for data retrieval:
  - Scrapes publicly accessible CNN articles
  - Extracts structured metadata (URL, date, title, author, content)
  - Sends the data to a local Java server via HTTP

- **Java** is responsible for data processing:
  - Receives article objects
  - Stores them as structured files
  - Enables future filtering, comparison, and analysis

This separation keeps scraping logic isolated from storage and analytics.

---

## Dataset Usage

Once collected, the dataset can be filtered or analyzed in any way you want, including:

- Keyword or topic filtering  
- Author or date range analysis  
- Trend analysis over time  
- Visualization or machine learning experiments  

---

## Legal & Ethical Notice ⚠️

- **Redistributing the generated dataset is not permitted**
- CNN content is protected by copyright
- This repository **does NOT include any scraped data**

### What is shared:
- ✅ The source code

### What is NOT shared:
- ❌ The generated dataset

You are responsible for generating your **own dataset** and complying with all applicable laws, website terms of service, and copyright restrictions.

---

## Environment & Performance

This project was tested on a **Raspberry Pi** (Java and Python come preinstalled and configured).

- Runs continuously with low resource usage  
- Collected **1,000+ articles in a few days**  
- No browser, WebDriver, or headless Chrome required  

---

