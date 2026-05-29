# Logo CarpoolApp - Diseño Vectorial

## Diseño Actual (ic_launcher_foreground.xml)

El logo representa los conceptos clave del carpooling:

### Elementos del diseño:
1. **Auto blanco** - Silueta simple vista desde arriba/lado
2. **Dos personas verdes** - Representan a conductor y pasajero (compartir)
3. **Ruta verde** - Línea curva debajo que simboliza el camino/ruta compartida
4. **Ventanas azules** - Dan profundidad al vehículo
5. **Ruedas azules** - Completan el diseño del auto
6. **Fondo azul oscuro** (#1E3A5F) - Contraste profesional

### Paleta de colores:
- **Azul oscuro** (#1E3A5F) - Fondo, confianza, profesionalismo
- **Blanco** (#FFFFFF) - Auto, claridad, limpieza
- **Verde** (#10B981) - Personas y ruta, sustentabilidad, comunidad
- **Azul claro** (#1A73E8) - Detalles, tecnología

## Alternativas de diseño

### Opción A: Logo con ruta y pin de ubicación
```xml
<!-- Ruta con pins de origen y destino -->
<path android:fillColor="#10B981" 
      android:pathData="M30,60C30,60 45,45 54,45C63,45 78,60 78,60"/>
```

### Opción B: Letra "C" estilizada como camino
```xml
<!-- C formada por una ruta -->
<path android:fillColor="#10B981"
      android:pathData="M70,40A30,30 0 1,0 70,80"/>
```

### Opción C: Ícono de manos compartiendo
```xml
<!-- Dos manos sosteniendo un auto pequeño -->
<path android:fillColor="#10B981"
      android:pathData="M40,60L54,50L68,60"/>
```

## Herramientas para editar el logo

### Editores gratuitos:
1. **Android Studio** - Editor vectorial integrado (Asset Studio)
2. **Vector Asset Studio** - Herramienta nativa de Android
3. **Inkscape** - Editor SVG gratuito, exporta a Android XML
4. **Figma** - Diseño vectorial online, plugin para Android

### Cómo modificar:
1. Abre Android Studio
2. Ve a `app/src/main/res/drawable`
3. Click derecho → New → Vector Asset
4. O edita directamente el XML

## Prompt para IA (si quieres generar una versión más profesional)

**Para DALL-E, Midjourney o Bing Image Creator:**
```
Minimalist app icon for carpooling community app, 
car silhouette with two people inside sharing a ride, 
modern flat vector design, blue and green colors, 
white background, suitable for Android adaptive icon, 
clean simple geometric shapes, professional tech startup style
```

**Para tools de logo con IA:**
- Looka.com: "Carpooling community ride sharing app"
- Hatchful.shopify.com: "Transportation → Car Service"
- Brandmark.io: "Car + People + Route"

## Próximos pasos sugeridos

1. ✅ **Prueba el diseño actual** en el emulador
2. ⬜ **Ajusta colores** si no coinciden con tu marca
3. ⬜ **Genera versión final** con IA o diseñador
4. ⬜ **Crea variantes** para notificaciones y splash screen

---

**Archivos relacionados:**
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - Logo principal
- `app/src/main/res/drawable/ic_launcher_background.xml` - Fondo
- `app/src/main/res/mipmap-*/ic_launcher.png` - Iconos generados
