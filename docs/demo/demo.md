# Demo Examples

## Hello World App

### Get It

[Download](https://api.hiconic-os.org/dowload-latest-artifact-part.php?groupId=hiconic.platform.reflex&artifactId=hello-world-app&classifier=application&type=zip) the application package unzip it to a folder of your choice 

### Run It

Change directory to the `bin` folder of the unzipped package and execute the `run` (bat/sh) script. A server will be started and you can try it out like this:

#### Using Browser

http://localhost:8080/api/main/Greet?name=fellow


#### Using CURL
```
curl http://localhost:8080/api/main/Greet?name=fellow
```

### Study It

* [Model](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/hello-world-model)
* [Application](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/hello-world-app)

## Reflex Demo Web App

### Get It

[Download](https://api.hiconic-os.org/dowload-latest-artifact-part.php?groupId=hiconic.platform.reflex&artifactId=demo-web-app&classifier=application&type=zip) the application package unzip it to a folder of your choice.

### Run It 

Change directory to the `bin` folder of the unzipped package and execute the `run` (bat/sh) script. A server will be started and you can try it out like this:

#### Using Browser

http://localhost:8080/api/main/ReverseText?text=hello

http://localhost:8080/api/main/GeneratePersons?personCount=100

#### Using CURL
```
curl http://localhost:8080/api/main/ReverseText?text=hello
```

```
curl http://localhost:8080/api/main/GeneratePersons?personCount=100
```

### Study It

* [Model](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/demo-model)
* [Module](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/demo-rx-module)
* [Application](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/demo-web-app)

## Reflex Demo CLI App

This application uses the same model and module as the Demo Web App but exposes it as a CLI endpoint to demonstrate the endpoint portability of a service module.

### Get It

[Download](https://api.hiconic-os.org/dowload-latest-artifact-part.php?groupId=hiconic.platform.reflex&artifactId=demo-cli-app&classifier=application&type=zip) the application package unzip it to a folder of your choice 

### Run It

Change directory to the `bin` folder of the unzipped package and execute:

```
run reverse-text --text hello
```

```
run generate-persons --personCount 100
```

### Study It

* [Model](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/demo-model)
* [Module](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/demo-rx-module)
* [Application](https://github.com/hiconic-os/hiconic.platform.reflex/tree/main/demo-cli-app)