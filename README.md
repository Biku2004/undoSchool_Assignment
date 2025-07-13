## Starting Elasticsearch
1. Ensure Docker is installed and running.
2. Run `docker-compose up -d` in your project folder containing docker file.
3. Verify Elasticsearch by running: `http://localhost:9200` on your browser.
4. This type of output will show when running the URL :
```
{
  "name": "2e6b1d5b7d13",
  "cluster_name": "docker-cluster",
  "cluster_uuid": "M0ssvgORT7ajgsKiI2i4hA",
  "version": {
    "number": "8.15.0",
    "build_flavor": "default",
    "build_type": "docker",
    "build_hash": "1a77947f34deddb41af25e6f0ddb8e830159c179",
    "build_date": "2024-08-05T10:05:34.233336849Z",
    "build_snapshot": false,
    "lucene_version": "9.11.1",
    "minimum_wire_compatibility_version": "7.17.0",
    "minimum_index_compatibility_version": "7.0.0"
  },
  "tagline": "You Know, for Search"
}
```
## Sample Data
- The `sample-courses.json` file is located in `src/main/resources`.
- It contains 50+ course objects with varied categories, types, prices, session dates etc.
- The application automatically loads this file to index data into Elasticsearch on startup.

## Assignment - A
### Build and Run the Application
1. Clone the repository: `git clone https://github.com/Biku2004/undoSchool_Assignment.git`
2. Navigate to the project directory: `cd course-search`
3. Build the project: `mvn clean install`
4. Run the application: `mvn spring-boot:run`
5. The application will start on `http://localhost:8080` and automatically index `sample-courses.json` into the `courses` index.

## API Endpoints

#### Search Courses
- **Endpoint**: `GET /api/search`
- **Parameters**:
  - `q`: Keyword for full-text search
  - `minAge`, `maxAge`: Age range filter
  - `category`: Exact category (e.g., "Math")
  - `type`: Exact type (e.g., "COURSE")
  - `minPrice`, `maxPrice`: Price range filter
  - `startDate`: ISO-8601 date (e.g., "2025-07-01T00:00:00Z")
  - `sort`: `upcoming` (default), `priceAsc`, `priceDesc`
  - `page`: Page number (default: 0)
  - `size`: Results per page (default: 10)
- **Example**:
  ```bash
    "http://localhost:8080/api/search?q=algebra&category=Math&minAge=10&maxAge=14"
  ```
Output Example : 
```
{
    "total": 2,
    "courses": [
        {
            "id": "course_1",
            "title": "Introduction to Algebra",
            "category": "Math",
            "price": 49.99,
            "nextSessionDate": "2025-07-15T10:00:00Z"
        },
        {
            "id": "course_37",
            "title": "Pre-Algebra Prep",
            "category": "Math",
            "price": 45.0,
            "nextSessionDate": "2025-08-10T10:00:00Z"
        }
    ]
}
```

  ```bash
    "http://localhost:8080/api/search?page=1&size=5&sort=priceAsc"
  ```
  
## Assignment B : Autocomplete Suggestions & Fuzzy Search
1. Replaced the original exact match logic with fuzzy search on the title field, enabling to get search results even on typos.
2. Implemented a new method(suggestTitles) and a field(suggest) o handle autocomplete suggestions.
3. Added API endpoints for theses to work.

### Autocomplete Suggestions

- **Endpoint**: `GET /api/search/suggest?q={partialTitle}`
- **Parameters**:
  - `q`: Keyword for auto complete suggestion

- **Example**:
  ```bash
    "http://localhost:8080/api/search/suggest?q=Staticstisx"
  ```
Output Example : 
```
[
    "Statistics for Beginners",
    "Probability and Statistics"
]
```


### Fuzzy Search

- **Endpoint**: `GET /api/search?q={Title}`
- **Parameters**:
  - `q`: Keyword for Searching the data

- **Example**:
  ```bash
    "http://localhost:8080/api/search?q=statistix"
  ```
Output Example : 
```
{
    "total": 2,
    "courses": [
        {
            "id": "course_43",
            "title": "Probability and Statistics",
            "category": "Math",
            "price": 68.0,
            "nextSessionDate": "2025-08-25T09:00:00Z"
        },
        {
            "id": "course_17",
            "title": "Statistics for Beginners",
            "category": "Math",
            "price": 65.0,
            "nextSessionDate": "2025-10-01T09:00:00Z"
        }
    ]
}
```




