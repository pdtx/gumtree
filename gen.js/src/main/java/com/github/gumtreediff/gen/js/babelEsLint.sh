#!/bin/bash

codePath=${1}
fileName=${2}

node babelEsLint.js ${codePath} ${fileName};