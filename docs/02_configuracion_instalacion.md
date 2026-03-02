# Instalando DspaceCris 8 desde el repositorio
Esta guia fue hecha usando un dispositivo con el sistema operativo 24.04 (LTS), por lo tanto, dependiendo del sistema 
operativo pueden haber algunas diferencias respecto a lo expresado aca. La guia oficial puede encontrarse en aca 
[dspace 8 wiki](https://wiki.lyrasis.org/display/DSDOC8x/Installing+Dspace).

## Usuarios a usar
- dspace
- root

## Softwares necesarios
Para la instalaccion se necesitan los siguientes softwares, se indican las versiones instaladas como referencia.
- Java JDK v17.0.10
- Maven 3.5.4
- Ant 1.10
- PostgreSQL 14
- Solr 8.11.1

Estos son softwares ampliamente usados por lo tanto se pueden encontrar en cualquier sistema de manejo de paquetes 
como. Cada elemento puede requerir una configuracion en especial, por lo tanto se recomienda revisar la 
[dspace 8 wiki](https://wiki.lyrasis.org/display/DSDOC8x/Installing+Dspace).

## Creando la base de datos
El primer paso es crear un usuario en el motor PostgreSQL, al cual llamaremos `dspace` usando 
```
createuser --username=postgres --no-superuser --pwprompt dspace
```
Luego, tenemos que crear la base de datos, con el perfil de superusuario, usando:
```
createdb --username=postgres --owner=dspace --encoding=UNICODE dspace
```
Y finalmente, tenemos que activar la `pgcrypto extension`, con el perfil de superusuario, usando:
```
psql --username=postgres dspace -c "CREATE EXTENSION pgcrypto;"
```

Los pasos anteriores se pueden lograr usando la `CLI` de PostgreSQL o directamente ejecutando los comandos apuntando a la base de datos.


## Instalando y ejecutando el backend
Para lo que sigue, vamos a usar al usuario `dspace` el cual debe tener permisos de lectura y escritura. Entonces, el 
primer paso es clonar el repositorio con `git clone`.

### Configuracion
El primer archivo que debemos editar es `[dspace-source]/dspace/config/local.cfg`. Dentro de esa carpeta hay un `local.cfg.EXAMPLE` el cual se tiene 
que generar una copia y renombrarla como `local.cfg`. Este es el archivo principal ya que reune toda la configuracion que tendra el backend del 
CRIS, por lo tanto, tiene muchas opciones para completar, pero aca solo indicaremos las mas importantes, e invitamos al lector a revisar las otras opciones.

En el siguiente bloque se resumen las opciones basicas que debe disponer el `local.cfg`, las otras que no fueron definidas aca son tomadas del `dspace.cfg`.
```
##########################
# SERVER CONFIGURATION   #
##########################
# This is the location where you want to install DSpace.
dspace.dir=/home/dspace/dspace

# Public URL of DSpace backend ('server' webapp). May require a port number if not using standard ports (80 or 443)
# DO NOT end it with '/'.
dspace.server.url = http://localhost:8080/server
dspace.server.ssr.url = ${dspace.server.url}

# Public URL of DSpace frontend (Angular UI). May require a port number if not using standard ports (80 or 443)
# DO NOT end it with '/'.
dspace.ui.url = http://localhost:4000

# Name of the site
dspace.name = CRIS Antartico

# Default language for metadata values
default.language = en_US

# Solr server/webapp.
solr.server = http://localhost:8983/solr


##########################
# DATABASE CONFIGURATION #
##########################
# DSpace ONLY supports PostgreSQL at this time.

# URL for connecting to database
db.url = jdbc:postgresql://localhost:5432/dspace

# Database username and password
db.username = <usuario>
db.password = <contraseña>
```

Un detalle importante, ademas de que las url deben estar operativas, es que la ruta definida en `dspace.dir` sea valida y tenga los permisos 
del usuario `dspace`.

### Compilacion
Cuando el archivo de configuracion esta listo, la base de datos operativa y la carpeta donde se almacenara el codigo compilada creada, podemos pasar 
al proceso de compilacion del sistema CRIS, el primer paso es construir el paquete usando `mvn package` en la ruta base de la carpeta del codigo clonado.
Si todo marcha sin problemas, nos debemos mover a la carpeta `[dspace-source]/dspace/target/dspace-installer` y ejecutar `ant fresh_install`, este comando 
instala el paquete en la carpeta definida en `dspace.dir`.


### Servicio de Sorl
Con Sorl operativo, el siguiente paso es agregar los archivos instalados al servicio de Solr usando `cp -rf [dspace.dir]/solr/* /var/solr/data/`. Despues 
es necesario reiniciar el servicio para que trabaje con la ultima version (`sudo systemctl restart solr`).


### Servicio de dspace

El siguiente paso es crear el servicio que conectara el backend, la vista web y la base de datos, para ello se tiene que crear un archivo en 
`/etc/systemd/system/dspace.service` y dentro de este colocar lo siguiente.

```
[Unit]
Description=DSpace-CRIS backend JAR running with embedded tomcat server
After=syslog.target network.target

[Service]
SuccessExitStatus=143

User=dspace
Group=dspace

Type=simple

Enviroment="JAVA_HOME=/home/dspace/.sdkman/candidates/java/current"
Enviroment="JAVA_OPTS=-Djava.security.egd=file:///dev/urandom -Xms512M -Xmx1024M -server -XX:+UseZGC - 
Dfile.encoding=UTF-8 -Djava.awt.headless=true -Duser.timezone=America/Punta_Arenas"
WorkingDirectory=/home/dspace/site/backend/

ExecStart=/bin/bash -c '/${JAVA_HOME}/bin/java -jar webapps/server-boot.jar'
ExecStop=/bin/kill -15 $MAINPID

[Install]
WantedBy=multi-user.target
```

Lo siguiente es levantar el servicio usando:
```
systemctl daemon-reload
systemctl start dspace.service
```

En caso de que todo este funcionando correctamente, al acceder a `http://localhost:8080/server` deberia mostrar el `The HAL browser`. Por otro 
lado, si al acceder a `http://localhost:8993` aparecen los multiples cores significa que Sorl identifico los cores.


### Comandos extras utiles
Con lo anterior, se tiene ya un backend operativo, pero hay otros comandos que son importantes de mencionar y que van a ser utiles 
durante el desarrollo. Para ello vamos a la carpeta definida en `dspace.dir`, dentro de esta podemos ejecutar los siguientes 
comandos.

Para crear un usuario con permisos de administrador:
```
bin/dspace create-administrator
```

Para inicializar las relaciones entre las diferentes entidades que componen el CRIS:
```
bin/dspace initialize-only-entity-types -d
bin/dspace initialize-entities -f config/entities/correction-relationship-types.xml
bin/dspace initialize-entities -f config/entities/hide-sort-relationship-types.xml
bin/dspace initialize-entities -f config/entities/relationship-types.xml
```

Para cargar la vista del CRIS:
```
bin/dspace cris-layout-tool -f config/layout/cris-layout-configuration.xls
```

Para cargar esquemas de metadatos.
```
bin/dspace registry-loader --all config/registries ##cargar todos
bin/dspace registry-loader -metadata <esquema> ##cargar uno en particular
```


Autor: Javier Norambuena Leiva
