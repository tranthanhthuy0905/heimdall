package models.hls

import java.util.UUID.randomUUID

import models.common.MediaIdent
import org.scalatestplus.play._

class HlsManifestFormatterSpec extends PlaySpec {

  "HLS Manifest Formatter" must {

    val mediaIdent = MediaIdent(List(randomUUID), List(randomUUID), randomUUID)

    val manifest =
      """#EXTM3U
        |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
        |/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false
        |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
        |/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false""".stripMargin

    "return an empty manifest" in {
      HlsManifestFormatter("", mediaIdent, "", None) mustBe ""
    }

    "return non-changed manifest" in {
      val partialManifest =
        """#EXTM3U
          |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368""".stripMargin

      HlsManifestFormatter(partialManifest, mediaIdent, "", None) mustBe partialManifest
    }

    "return manifest with replaced paths" in {
      HlsManifestFormatter(manifest, mediaIdent, "", None) mustBe
        """#EXTM3U
          |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
          |/media/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false
          |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
          |/media/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false""".stripMargin
    }

    "return manifest with replaced paths, and updated prefix" in {
      HlsManifestFormatter(manifest, mediaIdent, "/api/v1", None) mustBe
        """#EXTM3U
          |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
          |/api/v1/media/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false
          |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
          |/api/v1/media/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false""".stripMargin
    }

    "return manifest with replaced path at the eol" in {
      val eolManifest =
        """#EXTM3U
          |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
          |/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false
          |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
          |/hls/variant""".stripMargin

      HlsManifestFormatter(eolManifest, mediaIdent, "/api/v1", None) mustBe
        """#EXTM3U
          |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
          |/api/v1/media/hls/variant?file_id=4f7&evidence_id=31b&partner_id=f3d&level=0&autorotate=false
          |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
          |/api/v1/media/hls/variant""".stripMargin
    }

    "return manifest with replaced one source" in {
      val manifestWithSource =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/hls/variant?source=$randomUUID""".stripMargin

      HlsManifestFormatter(manifestWithSource, mediaIdent, "", None) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/media/hls/variant?${mediaIdent.toQueryString}""".stripMargin
    }

    "return manifest with replaced source in the beginning" in {
      val manifestWithSource =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/hls/variant?source=$randomUUID&level=0&autorotate=false""".stripMargin

      HlsManifestFormatter(manifestWithSource, mediaIdent, "", None) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/media/hls/variant?${mediaIdent.toQueryString}&level=0&autorotate=false""".stripMargin
    }
    
    "return manifest with replaced source in the middle" in {
      val manifestWithSources =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/hls/variant?level=0&source=$randomUUID&autorotate=false""".stripMargin

      HlsManifestFormatter(manifestWithSources, mediaIdent, "", None) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/media/hls/variant?level=0&${mediaIdent.toQueryString}&autorotate=false""".stripMargin
    }

    "return manifest with replaced source at the end" in {
      val manifestWithSources =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
           |/hls/variant?level=0&autorotate=false&source=$randomUUID""".stripMargin

      HlsManifestFormatter(manifestWithSources, mediaIdent, "", None) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
           |/media/hls/variant?level=0&autorotate=false&${mediaIdent.toQueryString}""".stripMargin
    }

    "return manifest with replaced source in the beginning with token" in {
      val manifestWithSource =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/hls/variant?source=$randomUUID&level=0&autorotate=false""".stripMargin

      HlsManifestFormatter(manifestWithSource, mediaIdent, "", Some("token")) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/media/hls/variant?${mediaIdent.toQueryString}&streamingSessionToken=token&level=0&autorotate=false""".stripMargin
    }

    "return manifest with replaced source in the middle with token" in {
      val manifestWithSources =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/hls/variant?level=0&source=$randomUUID&autorotate=false""".stripMargin

      HlsManifestFormatter(manifestWithSources, mediaIdent, "", Some("token")) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=2856770,RESOLUTION=856x368
           |/media/hls/variant?level=0&${mediaIdent.toQueryString}&streamingSessionToken=token&autorotate=false""".stripMargin
    }

    "return manifest with replaced source at the end with token" in {
      val manifestWithSources =
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
           |/hls/variant?level=0&autorotate=false&source=$randomUUID""".stripMargin

      HlsManifestFormatter(manifestWithSources, mediaIdent, "", Some("token")) mustBe
        s"""#EXTM3U
           |#EXT-X-STREAM-INF:BANDWIDTH=1384000,RESOLUTION=838x360
           |/media/hls/variant?level=0&autorotate=false&${mediaIdent.toQueryString}&streamingSessionToken=token""".stripMargin
    }

  }

}
