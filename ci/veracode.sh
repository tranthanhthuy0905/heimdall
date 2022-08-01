#!/bin/sh
# Produce scannable binaries for Veracode.
# For Scala, the universal ZIP binaries are known to work.

bin/sbt universal:packageBin
