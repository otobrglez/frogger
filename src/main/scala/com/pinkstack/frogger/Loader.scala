package com.pinkstack.frogger

import zio.ZIO
import zio.stream.{ZPipeline, ZStream}
import ZStream.fromPath

import java.nio.file.Path

object Loader:
  type Index = Long
  trait Decoder[T]:
    def decode(row: (Array[String], Index)): ZIO[Any, Throwable, T]

  private def columnSplitter(row: (String, Index)): (Array[String], Index) =
    (row._1.split(","), row._2)

  private def stream(path: Path): ZStream[Any, Throwable, (Array[String], Index)] =
    fromPath(path)
      .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      .zipWithIndex
      .drop(1)
      .map(columnSplitter)

  def streamAs[T](path: Path)(using decoder: Decoder[T]): ZStream[Any, Throwable, T] =
    stream(path).mapZIOParUnordered(8)(decoder.decode)
