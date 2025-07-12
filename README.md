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
### Sample Data
- The `sample-courses.json` file is located in `src/main/resources`.
- It contains 50+ course objects with varied categories, types, prices, session dates etc.
- The application automatically loads this file to index data into Elasticsearch on startup.