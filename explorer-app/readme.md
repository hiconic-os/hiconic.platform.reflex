
## Postgres Docker


```sh
docker run --name rx-explorer --rm -d -p 65432:5432 -e POSTGRES_DB=dbtest -e POSTGRES_USER=cortex -e POSTGRES_PASSWORD=cortex postgres:latest
```