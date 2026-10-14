# Sphere Music

Reproductor Android nativo de música local, inspirado en la facilidad de uso de
Lark Player, con diseño e identidad propios. Kotlin, Jetpack Compose y Media3.
Interfaz en español, tema oscuro, acentos violeta/verde y portadas generativas
cuando el archivo no tiene una portada disponible.

## Qué incluye esta versión

- Biblioteca del dispositivo mediante MediaStore: canciones, álbumes y búsqueda.
- Inicio simplificado con buscador y lista, sin cartel promocional.
- Barra de hora/batería oculta (reaparece temporalmente al deslizar desde arriba).
  Reproductor desplazable en pantallas pequeñas, horizontal o con letras grandes.
- Favoritos guardados localmente.
- Playlists locales: crear, renombrar, agregar/quitar canciones y eliminar listas.
  El menú de tres puntos está disponible en la biblioteca, cola y reproductor.
- Borrado real del audio desde ese menú, con confirmación previa y autorización
  de Android cuando corresponde. Se limpia la cola, favoritos y playlists;
  no se borran archivos de letras. Eliminar una playlist nunca borra sus audios.
- Orden predeterminado por fecha de incorporación a Android (más reciente
  primero), con opción A–Z. La elección se recuerda; no cambia una cola activa.
- Reproducción en segundo plano, notificación multimedia y controles de pantalla
  bloqueada; pausa al desconectar auriculares y gestión del foco de audio.
- Cola de reproducción, avance/retroceso, búsqueda temporal, mezcla y repetición.
- Arrastra el asa a la derecha de una canción en **Cola** para reordenarla, con
  desplazamiento automático en los bordes. TalkBack ofrece subir/bajar.
  Reordenar desactiva mezcla para respetar el orden manual, sin reiniciar el audio.
- Portadas de álbum disponibles en MediaStore.
- Letras incrustadas **USLT en MP3 ID3v2.3/v2.4**, o archivos `.lrc` / `.txt`
  vinculados desde el selector de archivos.
- Letras LRC sincronizadas, múltiples marcas de tiempo, offsets y salto al tocar
  una línea. Letras sin marcas de tiempo se muestran como texto.
- Sin anuncios, cuentas, servidores ni permiso de Internet.

## Descargar e instalar en Android

Requiere **Android 8.0 o posterior**.

1. Abre [Actions → Android APK](https://github.com/retr0zer07/Music-Play/actions/workflows/android.yml).
2. Selecciona una ejecución exitosa y descarga **Sphere-Music-Android** en
   Artifacts (GitHub requiere iniciar sesión para descargar artifacts).
3. Extrae el ZIP y abre `Sphere-Music.apk` en el teléfono.
4. Autoriza la instalación desde ese navegador o gestor de archivos si Android
   lo solicita. Abre Sphere Music y permite el acceso a música.
5. Copia música al teléfono, por ejemplo a la carpeta `Music`, y toca actualizar.

Las versiones etiquetadas `v*` también publican el APK en
[Releases](https://github.com/retr0zer07/Music-Play/releases), sin necesidad de
descargar un artifact. GitHub aloja el código y el APK, **no ejecuta la app**;
GitHub Pages no es necesario para una aplicación nativa.

### Archivos que aparecen en la biblioteca

La biblioteca incluye **todo el audio indexado por Android en almacenamiento
compartido**, aunque Android no lo marque como música; también puede incluir
grabaciones, tonos y podcasts. No se aplica un límite de duración ni de cantidad.
El botón actualizar vuelve a consultar el índice de audio, incluyendo los
volúmenes externos que Android expone en MediaStore.

Los archivos en carpetas privadas de otras apps, carpetas con `.nomedia`,
descargas offline cifradas o archivos todavía no indexados no son accesibles
mediante ese permiso. Si falta un archivo, cópialo a una carpeta pública como
`Music`, espera a que Android lo indexe y actualiza la biblioteca. No solicitamos
acceso total al almacenamiento ni prometemos acceso a archivos protegidos.

### Portadas y letras

Las portadas dependen de que Android haya indexado el arte del álbum. Si no están
disponibles, aparece una portada generada por la app. Para letras externas:

1. Reproduce una canción y abre **Letras**.
2. Toca **Vincular letras (.lrc / .txt)** y selecciona el archivo correspondiente.
3. La vinculación permanece guardada para esa canción. Cuando hay letra, los
   botones de vincular/cambiar carpeta se ocultan. Para sustituir una letra,
   usa **Vincular letras** en el menú de tres puntos del reproductor; la carpeta
   sigue disponible en la cabecera de la biblioteca.

### Letras automáticas por nombre

Toca la carpeta en la cabecera o **Carpeta de letras automáticas** en Letras.
Autoriza una carpeta una sola vez: se buscan archivos `.lrc` en ella y sus
subcarpetas, con permiso recordado. En Android 10+ se consulta primero **la misma
carpeta del archivo de audio**, si está dentro de la carpeta autorizada, usando
su ubicación y volumen de MediaStore. Autoriza la carpeta de tu música o una
carpeta superior; el permiso de audio por sí solo no permite leer los `.lrc`.
En otros proveedores/Android 8–9 se mantiene la búsqueda por nombre dentro del
árbol autorizado.

Se compara primero el nombre del archivo de audio sin extensión y luego su
título, sin distinguir mayúsculas. También se admite la variante de SnapTube
`nombre_private.lrc`, con preferencia por `nombre.lrc`. Ejemplo:
`Mi tema.mp3` → `Mi tema.lrc`. Se conserva la puntuación y se normaliza Unicode.
Si dos archivos tienen el mismo nombre, vincula uno manualmente para evitar
mostrar una letra incorrecta. Los vínculos manuales tienen prioridad; después
van los LRC automáticos y, si no hay coincidencia, las letras incrustadas.
La carpeta vecina se consulta al cargar la canción y las búsquedas sin resultado
refrescan el índice para detectar nuevas descargas. Toca actualizar para volver
a cargar letras de una canción que ya esté abierta. Si revocas el permiso o borras
archivos vinculados, la app avisa; vuelve a seleccionar la carpeta/archivo.

El selector admite proveedores locales y de documentos. El límite es **1 MB**;
se admiten UTF-8 y UTF-16 con BOM. Android moderno restringe el acceso directo
a archivos vecinos al audio: solo se buscan automáticamente `.lrc` dentro de
la carpeta autorizada y no se solicita acceso total al almacenamiento. Android
puede impedir seleccionar la raíz de almacenamiento o Descargas; usa una
subcarpeta como `Music/Letras` y concede acceso mediante el selector.

La búsqueda no limita la cola al resultado visible: al seleccionar una canción
se carga la colección actual (biblioteca, álbum o favoritos) para poder activar
la mezcla dentro del reproductor. La selección de mezcla se conserva al elegir
otra canción durante la sesión.
Al activar mezcla se genera un nuevo recorrido desde la canción actual, sin
reiniciarla; la siguiente no será la sucesora del orden normal si hay al menos
tres canciones. Cada canción se visita una vez por recorrido. Activar mezcla
también desactiva repetir una sola canción; puedes volver a elegir repetición.

### Playlists y borrado

En **Playlists**, toca **Crear playlist** y escribe un nombre único. Desde los
tres puntos de una canción, **Agregar a playlist** permite elegir una lista
existente o crear una con esa canción. Las listas conservan el orden de adición,
no duplican una canción y se guardan en el teléfono. Sus opciones permiten
renombrarlas o eliminarlas; dentro de una lista puedes quitar una canción sin
borrar su archivo.

**Borrar del teléfono** elimina solamente el audio seleccionado. Cancelar
cualquiera de las confirmaciones conserva el archivo y sus referencias.
Android 11+ muestra su diálogo de borrado; Android 10 puede pedir permiso
específico sobre el archivo. Android 8–9 requiere permiso de escritura en
almacenamiento, solicitado únicamente al intentar borrar.

El logo e icono adaptativo se generan a partir de `logo.svg`, conservando sus
formas, colores y degradado. Android 13+ también admite icono temático.

Ejemplo de LRC (texto original de demostración):

```text
[ti:Mi universo]
[ar:Sphere]
[offset:0]
[00:02.00]Una luz en mi universo
[00:06.50]Cada nota encuentra su lugar
```

## Desarrollo y validación

Necesitas JDK 17 y Android SDK con la plataforma 35 y build-tools 34.0.0.
Abre el proyecto en Android Studio o configura `ANDROID_HOME` / `local.properties`.

En Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

En Linux/macOS:

```sh
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.
Las pruebas cubren el parser LRC, lectura acotada de letras MP3 y formato de tiempo.
También cubren coincidencias de nombre, prioridad de archivo sobre título,
Unicode y rechazo de letras ambiguas.
Con un teléfono o emulador conectado, `:app:connectedDebugAndroidTest` comprueba
que también se incluya audio con `IS_MUSIC=0` y que la biblioteca coincida con
todo el índice accesible de MediaStore. El test crea un WAV silencioso temporal
y lo elimina al terminar. CI compila este APK de pruebas, pero su ejecución
requiere un dispositivo.

### GitHub Actions y publicación

El workflow se ejecuta en cada push a `main`, pull request y ejecución manual.
Compila, ejecuta pruebas y lint, y conserva el APK durante 30 días.
Para una versión descargable permanente, crea y sube una etiqueta:

```sh
git tag v0.1.0
git push origin v0.1.0
```

**Los APK publicados son previews de depuración**, no una distribución de
producción ni una publicación en Google Play. Para producción se necesita una
clave de firma estable guardada en GitHub Secrets, nunca en el repositorio,
y un flujo de firma de release. Los APK debug de ejecuciones diferentes pueden
usar claves distintas: Android podría exigir desinstalar una preview anterior
antes de instalar otra, perdiéndose favoritos y vínculos de letras.

## Alcance y comprobación en dispositivo

Esta primera versión no incluye streaming, descargas de música/letras,
ecualizador, playlists editables ni lectura de letras incrustadas en formatos
distintos de USLT MP3. Los formatos de audio reproducibles dependen de los
decodificadores de Android/Media3. La cola se mantiene mientras el servicio está
activo; no se restaura después de que Android termina el proceso.

Antes de distribuir, comprobar en un teléfono Android 8 y uno Android 13+:

- Permiso concedido, denegado y habilitado desde ajustes.
- Biblioteca vacía, búsqueda, álbumes, favoritos y actualización tras copiar música.
- Portadas presentes/ausentes, audio dañado o archivo eliminado.
- Reproducción con pantalla apagada, controles de notificación y auriculares.
- Mezcla, repetición, cola y desplazamiento temporal.
- Letras USLT y LRC con offsets, selección de TXT y vínculo conservado al reabrir.
- Tamaños de pantalla pequeños, orientación horizontal y fuentes grandes.

La preview inicial se comprobó en un Samsung SM-S906U1 con Android 16:
inicio, permiso denegado/concedido, indexación y portadas, reproducción de un WAV
original de prueba, favoritos, mezcla, repetición, cola, servicio en segundo plano,
pausa y vinculación de un LRC mediante el selector de Android. Tocar una línea
del LRC desplazó la reproducción a su marca exacta de 30 segundos. GitHub Actions
ejecuta las 11 pruebas unitarias, lint y la compilación del APK. Esto no sustituye
la comprobación de todos los dispositivos y casos de la lista anterior.

En 0.1.1 se añadieron pruebas instrumentadas del índice de audio completo y del
orden de los controles (sin solaparse con las pestañas). El reproductor también
se verifica en vertical y horizontal con un WAV silencioso temporal, restaurando
la orientación original del dispositivo al terminar.

En 0.1.2 hay 15 pruebas unitarias y 4 instrumentadas. En el teléfono se comprobó
el orden reciente/A–Z y su persistencia, la autorización de una carpeta y la
carga automática de LRC en subcarpetas después de reiniciar el proceso. También
se verificó que arrastrar cambie el siguiente audio de la cola real y que mover
la canción actual conserve identidad, estado de pausa y posición exacta de
30 segundos. El nuevo logo SVG se comprobó visualmente en la cabecera.

La app solo accede al audio autorizado y a los documentos seleccionados.
Favoritos y vínculos se guardan en preferencias privadas, sin copia de seguridad.
No incluye música, portadas comerciales ni letras protegidas: usa tus propios
archivos y contenido que tengas derecho a utilizar.