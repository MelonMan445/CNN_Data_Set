"""
CNN Article Scraper - curl_cffi Version
NO browsers, NO drivers, TLS fingerprint evasion
"""

import time
import random
import re
from datetime import datetime

from curl_cffi import requests
from bs4 import BeautifulSoup


class CNNScraper:

    def __init__(self, java_url="http://localhost:8080/api/articles"):
        self.base_url = "https://www.cnn.com"
        self.java_url = java_url
        self.scraped_urls = set()

        # curl_cffi session impersonating real Chrome
        self.session = requests.Session(
            impersonate="chrome120",
            timeout=30
        )

    def is_article_url(self, url):
        return bool(re.search(r'/\d{4}/\d{2}/\d{2}/', url))

    def fetch(self, url):
        # Human-like jitter
        time.sleep(random.uniform(1.5, 3.0))

        return self.session.get(
            url,
            headers={
                "Accept-Language": "en-US,en;q=0.9",
                "Referer": "https://www.google.com/"
            }
        )

    def find_article_links(self, html):
        soup = BeautifulSoup(html, "html.parser")
        links = set()

        for a in soup.find_all("a", href=True):
            href = a["href"]

            if href.startswith("/"):
                full_url = self.base_url + href
            elif href.startswith("http"):
                full_url = href
            else:
                continue

            if "cnn.com" in full_url and self.is_article_url(full_url):
                clean = full_url.split("?")[0].split("#")[0]
                links.add(clean)

        return links

    def scrape_article(self, url):
        try:
            print(f"\n🌐 Scraping: {url[:80]}")

            r = self.fetch(url)
            if r.status_code != 200:
                print(f"❌ HTTP {r.status_code}")
                return None

            soup = BeautifulSoup(r.text, "html.parser")

            for tag in soup(["script", "style", "nav", "header", "footer", "aside"]):
                tag.decompose()

            # TITLE
            title = None
            h1 = soup.find("h1")
            if h1:
                title = h1.get_text(strip=True)

            if not title:
                meta = soup.find("meta", property="og:title")
                if meta:
                    title = meta.get("content")

            if not title:
                return None

            # AUTHOR
            author = "CNN"
            meta_author = soup.find("meta", attrs={"name": "author"})
            if meta_author:
                author = meta_author.get("content")

            # DATE
            date = None
            meta_date = soup.find("meta", property="article:published_time")
            if meta_date:
                date = meta_date.get("content")

            if not date:
                match = re.search(r'/(\d{4})/(\d{2})/(\d{2})/', url)
                if match:
                    date = f"{match.group(1)}-{match.group(2)}-{match.group(3)}"

            if not date:
                date = datetime.now().isoformat()

            # CONTENT
            content_parts = []

            article = soup.find("article")
            paragraphs = article.find_all("p") if article else soup.find_all("p")

            for p in paragraphs:
                text = p.get_text(strip=True)
                if len(text) > 40 and not any(
                    bad in text.lower()
                    for bad in ["subscribe", "cookie", "newsletter", "advertisement"]
                ):
                    content_parts.append(text)

            content = "\n\n".join(content_parts)

            if len(content) < 200:
                return None

            article_data = {
                "url": url,
                "title": title,
                "author": author,
                "date": date,
                "content": content
            }

            print(f"✅ {title[:60]}...")
            print(f"✍️ {author} | 🗓 {date}")
            print(f"📄 {len(content.split())} words")

            return article_data

        except Exception as e:
            print(f"❌ Error: {e}")
            return None

    def send_to_java(self, article):
        try:
            r = self.session.post(
                self.java_url,
                json=article,
                headers={"Content-Type": "application/json"}
            )

            if r.status_code == 201:
                print("💾 Saved to Java")
                return True
            elif r.status_code == 409:
                print("⚠️ Already exists")
                return False
            else:
                print(f"❌ Java error: {r.status_code}")
                return False

        except Exception as e:
            print(f"❌ Java unreachable: {e}")
            return False

    def run(self, scan_interval=300, max_per_scan=15):
        print("🚀 CNN Scraper (NO DRIVERS) Started")

        while True:
            try:
                print("\n📥 Fetching CNN homepage")
                r = self.fetch(self.base_url)
                if r.status_code != 200:
                    print("❌ Homepage blocked")
                    time.sleep(60)
                    continue

                links = self.find_article_links(r.text)
                new_links = [u for u in links if u not in self.scraped_urls][:max_per_scan]

                print(f"🔗 New articles: {len(new_links)}")

                for url in new_links:
                    time.sleep(random.uniform(6, 10))

                    article = self.scrape_article(url)
                    if article:
                        self.send_to_java(article)
                        self.scraped_urls.add(url)

                print(f"⏳ Sleeping {scan_interval}s")
                time.sleep(scan_interval)

            except KeyboardInterrupt:
                print("\n🛑 Stopped by user")
                break

            except Exception as e:
                print(f"❌ Loop error: {e}")
                time.sleep(60)


if __name__ == "__main__":
    scraper = CNNScraper(
        java_url="http://localhost:8080/api/articles"
    )
    scraper.run(scan_interval=3, max_per_scan=15)
