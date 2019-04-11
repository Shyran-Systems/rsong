package coop.rchain.rsong.core.domain

import com.google.protobuf.ByteString
import coop.rchain.crypto.{ PrivateKey, PublicKey }

sealed trait Domain

case class RSongJsonAsset(
  id: String,
  assetData: String,
  jsonData: String
) extends Domain

case class Server(
  hostName: String,
  port: Int
) extends Domain

case class RholangContract(
  code: String,
  privateKey: PrivateKey,
  publicKey: PublicKey,
  algorithm: String = "Ed25519"
) extends Domain

case class Content(
  id: String,
  uri: String
) extends Domain

case class Artwork(
  id: String,
  uri: String
) extends Domain

case class Artist(
  id: String,
  title: String,
  name: String
) extends Domain

case class Audio(
  effect: String,
  uri: String,
  duration_ms: Long
) extends Domain

case class Song(
  id: String,
  title: String,
  name: String,
  audio: List[Audio],
  language: String
) extends Domain

case class Album(
  id: String,
  title: String,
  name: String,
  artworks: List[Artwork],
  duration_ms: Long,
  artists: List[Artist],
  uri: String
) extends Domain

case class SongMetadata(
  song: Song,
  artists: List[Artist],
  artwork: List[Artwork],
  album: Option[Album] = None
) extends Domain
