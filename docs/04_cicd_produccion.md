# Entorno de produccion
En este archivo se busca entregar los detalles generales de como se levanto el 
entorno de produccion en la maquina virtual de Digital Ocean contratada en el 
proyecto Nodo en la primera iteracion.


## Pipeline
Para el despliegue automatico de nuevas versiones se utiliza github action mediante 
un pipeline de definido en un `.yml` en la carpeta `.github/workflows`. En este se 
definen 5 trabajos.

Estos trabajos se ejecutan en un `self-hosted runner`, el cual es un servicio que se 
esta ejecutando en la maquina virtual en la nube en Digital Ocean. Por lo tanto, los comandos 
en el pipeline se ejecutan directamente en los archivos sin necesidad de conexion mediante 
`ssh` u otro medio.

En estos trabajos se automatiza la instalacion indicada en la wiki del 
[dspace 8 wiki](https://wiki.lyrasis.org/display/DSDOC8x/Installing+Dspace). Agregando 
comandos para limpiar el entorno y no dejar archivos que puedan ensuciar el entorno de 
produccion.

## Proxy inverso
Para la configuracion de direcciones se utiliza `nginx` para construir un proxy inverso, y `Certbot` 
para que maneje los certificados para request `https`. 
A continuacion se puede ver como luce la configuracion para el backend, el cual se puede encontrar en 
`/etc/nginx/sites-available/default`.

```
server {
    server_name $SERVER_NAME;

    location /server {
        proxy_pass $URL_BACKEND;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Origin $http_origin;
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate $RUTA_fullchain.pem; # managed by Certbot
    ssl_certificate_key $RUTA_privkey.pem; # managed by Certbot
    include /$RUTA_options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam $RUTA_ssl-dhparams.pem; # managed by Certbot

}

}server {
    if ($host = $SERVER_URL) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    listen 80;
    server_name $SERVER_NAME;
    return 404; # managed by Certbot
}
```


## Futuras consideraciones
Para futuros depliegues de la infraestructura, se recomienda dejar el `runner` en una 
maquina diferente a la usada en produccion, esto puede implicar editar un poco el 
pipeline, agregando un cliente `ssh` o algo de ese estilo.

Por otro lado, para el proxy inverso, es recomendable trabajar con diferentes archivos y no 
el archivo que viene por `default`.

Autor: Javier Norambuena Leiva
