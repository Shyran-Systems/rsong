package coop.rchain.rsong.core.repo

import coop.rchain.rsong.core.domain.{Err, OpCode, Query}
import com.typesafe.scalalogging.Logger

trait AssetRepo {
  def getAsset(q: Query): Either[Err, String]
  def dataAtName(name: String, depth: Int) : Either[Err, String]
  def findDataAtName(name: String)(maxDepth: Int) : Either[Err, String] = {
    def helper(depth: Int) : Either[Err, String] = {
      println(s"_______________ DEPTH = $depth.  name= $name ----------------")
      dataAtName(name, depth) match {
        case Left(Err(OpCode.nameNotFound, _)) if depth < maxDepth => helper(depth+1)
        case Right(r) => Right(r)
        case Left(e) => Left(e)
      }
      }
    helper( 0)
  }
}

object AssetRepo {
  lazy val log = Logger[AssetRepo.type]

  def apply(proxy: RNodeProxy): AssetRepo = new AssetRepo {
      def dataAtName(name: String, depth: Int): Either[Err, String] =
        proxy.dataAtName(name, depth)

      def getAsset(q: Query): Either[Err, String] = 
      for {
        _ ← proxy.deploy(q.contract)
        _ ← proxy.proposeBlock
        asStr ← findDataAtName(s""""${q.nameOut}"""")(20)
      } yield (asStr)
    }

}
