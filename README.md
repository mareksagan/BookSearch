# BookSearch
Search engine for books written in Apache Solr, IText 7, Java 8 and VueJS

## Requirements
* API will expose an endpoint which accepts the search query and returns the page results
* Another endpoint will enable the user to index a **PDF** file page by page
* There will be a user interface making the project user friendly

## Search engine document schema
| Attribute | Description         |
| ----------| ------------------- |
| page      | Page number         |
| text      | Extracted page text |
| title     | Book title          |
| author    | Book author name    |

## Installation
* Set up [Maven](https://maven.apache.org/download.cgi) and [JDK 11](https://adoptopenjdk.net/) on your machine
* Run `mvn clean install`
* Run `mvn package` to deploy a JAR file
