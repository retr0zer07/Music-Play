# Sphere Music

Reproductor Android nativo de música local, inspirado en la facilidad de uso de
Lark Player, con diseño e identidad propios. Kotlin, Jetpack Compose y Media3.
Interfaz en español, tema oscuro, acentos violeta/verde y portadas generativas
cuando el archivo no tiene una portada disponible.

## Qué incluye esta versión

- Biblioteca del dispositivo mediante MediaStore: canciones, álbumes y búsqueda.
- Favoritos guardados localmente.
- Reproducción en segundo plano, notificación multimedia y controles de pantalla
  bloqueada; pausa al desconectar auriculares y gestión del foco de audio.
- Cola de reproducción, avance/retroceso, búsqueda temporal, mezcla y repetición.
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

### Portadas y letras

Las portadas dependen de que Android haya indexado el arte del álbum. Si no están
disponibles, aparece una portada generada por la app. Para letras externas:

1. Reproduce una canción y abre **Letras**.
2. Toca **Vincular letras (.lrc / .txt)** y selecciona el archivo correspondiente.
3. La vinculación permanece guardada para esa canción. Puedes cambiarla
   seleccionando otro archivo.

El selector admite proveedores locales y de documentos. El límite es **1 MB**;
se admiten UTF-8 y UTF-16 con BOM. Android moderno restringe el acceso directo
a archivos vecinos al audio: **no se buscan automáticamente archivos `.lrc`**
ni se solicita acceso total al almacenamiento.

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

La app solo accede al audio autorizado y a los documentos seleccionados.
Favoritos y vínculos se guardan en preferencias privadas, sin copia de seguridad.
No incluye música, portadas comerciales ni letras protegidas: usa tus propios
archivos y contenido que tengas derecho a utilizar.