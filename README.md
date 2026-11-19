# Traductor del lenguaje Boreal (Grupo 8)

Práctica de **Traductores de Lenguajes** (ETSIINF-UPM, curso 2025-26). Es un **traductor descendente recursivo** completo, en **Java**, para el lenguaje *Boreal*. Cubre todas las fases:

- **Procesador**: analizador léxico (`ALex`), analizador sintáctico descendente recursivo (`ASin`), acciones semánticas y tabla de símbolos (`tslib`).
- **Generación de código intermedio** en cuartetos (`GenCodigoIntermedio`, `Cuarteto`).
- **Generación de código objeto** para el ensamblador ENS2001 (`GenCodigoObjeto`), con diseño de registros de activación.

## Estructura
- `Traductores-de-Lenguajes-newDev/PdLDesRecBoreal - 1.01/AnalizadorDescendente/src/`: código fuente.
- `.../AnalizadorDescendente/ejemplos/`: programas Boreal de prueba.
- `Memoria_Traductor_Grupo_8.docx`: memoria del traductor.
- `Traductores-de-Lenguajes-newDev/docs/`: documentación de ENS2001 y del formato de cuartetos.

## Créditos
Práctica en grupo (Grupo 8). Parte del procesador descendente recursivo *PdLDesRecBoreal* proporcionado en la asignatura (autora: Inés Pérez).
