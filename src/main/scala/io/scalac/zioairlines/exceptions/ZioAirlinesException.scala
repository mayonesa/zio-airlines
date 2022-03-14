package io.scalac.zioairlines.exceptions

abstract class ZioAirlinesException private[exceptions] (msg: String) extends Exception(msg)
