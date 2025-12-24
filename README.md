# CNN_Data_Set
CNN Dataset Creator

This project is a news article dataset generator built using Python (scraping) and Java (storage & processing).
It automatically collects CNN articles and structures them into a clean dataset containing:

URL

Publication date

Title

Author

Full article content

The goal of this project is data analysis, filtering, and visualization, not redistribution.

How It Works

Python handles data retrieval:

Scrapes publicly accessible CNN articles

Extracts structured metadata (URL, date, title, author, content)

Sends the data to a local Java server via HTTP

Java handles data processing:

Receives article objects

Stores them as structured files

Allows future filtering, comparison, or analysis

This separation keeps scraping logic isolated from data storage and analysis.

Dataset Usage

Once collected, the dataset can be filtered or analyzed in any way you want, for example:

Keyword or topic filtering

Author or date range analysis

Trend analysis over time

Visualization or machine learning experiments

Legal & Ethical Notice ⚠️

Redistributing the collected dataset is not permitted.

CNN content is protected by copyright.

This repository does NOT include any scraped data.

What is shared:

✅ The code

❌ The dataset itself

You are responsible for generating your own dataset and complying with all applicable laws, terms of service, and copyright restrictions.

Environment & Performance

This system was tested on a Raspberry Pi (Java and Python come preinstalled and configured).

Ran continuously for a few days

Collected 1,000+ articles

Lightweight and low-power friendly

No browser or WebDriver required

Disclaimer

This project is for educational and research purposes only.
The author is not responsible for misuse of the code or violations of third-party terms.
