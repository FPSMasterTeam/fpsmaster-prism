# Shared module icons

ClickGUI module icons use Lucide's 24×24, 2px rounded line style. SVG files in `modules/` are the
source of truth; runtime PNGs are baked into the Prism resource JAR at 24, 48, and 96 pixels.

Regenerate SVG sources with `tools/icons/generate_module_icons.py`. To bake runtime textures, place
svgSalamander 1.1.4 beside `tools/icons/Bake.java`, then run:

```bash
cd tools/icons
java -cp svgSalamander.jar Bake.java
```

Lucide is distributed under the ISC license. See `LUCIDE_LICENSE.txt`.
