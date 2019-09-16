package services.rti.metadata

import play.api.libs.json._

trait MetadataValue {
  def displayValue: String
}

sealed trait CompressionValue extends MetadataValue {
  def value: Int
}

case class DateTime(dateTime: String) extends MetadataValue {
  val displayValue = "Modified Date"
}

object DateTime extends MetadataJsonFields {
  implicit val dateTimeReads: Reads[DateTime] = (__ \ dateTimeField).read[String].map(DateTime.apply)

  implicit val dateTimeWrites: Writes[DateTime] = writesBuilder((data: DateTime) => (data.displayValue, data.dateTime))
}

case class ImageDescription(desc: String) extends MetadataValue {
  val displayValue = "Image Description"
}

object ImageDescription extends MetadataJsonFields {
  implicit val imageDescriptionReads: Reads[ImageDescription] =
    (__ \ imageDescriptionField).read[String].map(ImageDescription.apply)

  implicit val imageDescriptionWrites: Writes[ImageDescription] = writesBuilder(
    (data: ImageDescription) => (data.displayValue, data.desc))
}

case class Software(software: String) extends MetadataValue {
  val displayValue = "Software"
}

object Software extends MetadataJsonFields {
  implicit val softwareReads: Reads[Software] = (__ \ softwareField).read[String].map(Software.apply)

  implicit val softwareWrites: Writes[Software] = writesBuilder((data: Software) => (data.displayValue, data.software))
}

case class Orientation(orientation: String) extends MetadataValue {
  val displayValue = "Orientation"
}

object Orientation extends MetadataJsonFields with ReadableValue {
  implicit val orientationReads: Reads[Orientation] = (__ \ orientationField).read[String].map(Orientation.apply)

  implicit val orientationWrites: Writes[Orientation] = writesBuilder(
    (data: Orientation) => (data.displayValue, fromString(data.orientation))
  )

  case object Horizontal extends MetadataValue {
    val value        = 1
    val displayValue = "Horizontal"
  }
  private val someHorizontal = Some(Horizontal)

  case object MirrorHorizontal extends MetadataValue {
    val value        = 2
    val displayValue = "Mirror horizontal"
  }
  private val someMirrorHorizontal = Some(MirrorHorizontal)

  case object Rotate180 extends MetadataValue {
    val value        = 3
    val displayValue = "Rotate 180"
  }
  private val someRotate180 = Some(Rotate180)

  case object MirrorVertical extends MetadataValue {
    val value        = 4
    val displayValue = "Mirror vertical"
  }
  private val someMirrorVertical = Some(MirrorVertical)

  case object MirrorHorizontalAndRotate270CW extends MetadataValue {
    val value        = 5
    val displayValue = "Mirror horizontal and rotate 270 CW"
  }
  private val someMirrorHorizontalAndRotate270CW = Some(MirrorHorizontalAndRotate270CW)

  case object Rotate90CW extends MetadataValue {
    val value        = 6
    val displayValue = "Rotate 90 CW"
  }
  private val someRotate90CW = Some(Rotate90CW)

  case object MirrorHorizontalAndRotate90CW extends MetadataValue {
    val value        = 7
    val displayValue = "Mirror horizontal and rotate 90 CW"
  }
  private val someMirrorHorizontalAndRotate90CW = Some(MirrorHorizontalAndRotate90CW)

  case object Rotate270CW extends MetadataValue {
    val value        = 8
    val displayValue = "Rotate 270 CW"
  }
  private val someRotate270CW = Some(Rotate270CW)

  /**
    * Find the enum by its integer value
    * Return None if the value is not found
    * */
  def getMetadataValue(value: Int): Option[MetadataValue] =
    value match {
      case Horizontal.value                     => someHorizontal
      case MirrorHorizontal.value               => someMirrorHorizontal
      case Rotate180.value                      => someRotate180
      case MirrorVertical.value                 => someMirrorVertical
      case MirrorHorizontalAndRotate270CW.value => someMirrorHorizontalAndRotate270CW
      case Rotate90CW.value                     => someRotate90CW
      case MirrorHorizontalAndRotate90CW.value  => someMirrorHorizontalAndRotate90CW
      case Rotate270CW.value                    => someRotate270CW
      case _                                    => None
    }
}

case class PixelXDimension(x: String) extends MetadataValue {
  val displayValue = "Image Width"
}

object PixelXDimension extends MetadataJsonFields {
  implicit val pixelXDimensionReads: Reads[PixelXDimension] =
    (__ \ pixelXDimensionField).read[String].map(PixelXDimension.apply)

  implicit val pixelXDimensionWrites: Writes[PixelXDimension] = writesBuilder(
    (data: PixelXDimension) => (data.displayValue, data.x))
}

case class PixelYDimension(y: String) extends MetadataValue {
  val displayValue = "Image Height"
}

object PixelYDimension extends MetadataJsonFields {
  implicit val pixelYDimensionReads: Reads[PixelYDimension] =
    (__ \ pixelYDimensionField).read[String].map(PixelYDimension.apply)

  implicit val pixelYDimensionWrites: Writes[PixelYDimension] = writesBuilder(
    (data: PixelYDimension) => (data.displayValue, data.y))
}

case class XResolution(x: String) extends MetadataValue {
  val displayValue = "X-Resolution"
}

object XResolution extends MetadataJsonFields {
  implicit val xResolutionReads: Reads[XResolution] = (__ \ xResolutionField).read[String].map(XResolution.apply)

  implicit val xResolutionWrites: Writes[XResolution] = writesBuilder(
    (data: XResolution) => (data.displayValue, data.x))
}

case class YResolution(y: String) extends MetadataValue {
  val displayValue = "Y-Resolution"
}

object YResolution extends MetadataJsonFields {
  implicit val yResolutionReads: Reads[YResolution] = (__ \ yResolutionField).read[String].map(YResolution.apply)

  implicit val yResolutionWrites: Writes[YResolution] = writesBuilder(
    (data: YResolution) => (data.displayValue, data.y))
}

case class Compression(compression: String) extends MetadataValue {
  val displayValue = "Compression"
}

object Compression extends MetadataJsonFields with ReadableValue {
  implicit val compressionReads: Reads[Compression] = (__ \ compressionField).read[String].map(Compression.apply)

  implicit val compressionWrites: Writes[Compression] = writesBuilder(
    (data: Compression) => (data.displayValue, fromString(data.compression))
  )

  case object Uncompressed extends CompressionValue {
    val value        = 1
    val displayValue = "Uncompressed"
  }

  case object CCITT1D extends CompressionValue {
    val value        = 2
    val displayValue = "CCITT 1D"
  }

  case object T4Group3Fax extends CompressionValue {
    val value        = 3
    val displayValue = "T4/Group 3 Fax"
  }

  case object T6Group4Fax extends CompressionValue {
    val value        = 4
    val displayValue = "T6/Group 4 Fax"
  }

  case object LZW extends CompressionValue {
    val value        = 5
    val displayValue = "LZW"
  }

  case object JPEGOldStyle extends CompressionValue {
    val value        = 6
    val displayValue = "JPEG (old-style)"
  }

  case object JPEG1 extends CompressionValue {
    val value        = 7
    val displayValue = "JPEG"
  }

  case object AdobeDeflate extends CompressionValue {
    val value        = 8
    val displayValue = "Adobe Deflate"
  }

  case object JBIGBAndW extends CompressionValue {
    val value        = 9
    val displayValue = "JBIG B&W"
  }

  case object JBIGColor extends CompressionValue {
    val value        = 10
    val displayValue = "JBIG Color"
  }

  case object JPEG2 extends CompressionValue {
    val value        = 99
    val displayValue = "JPEG"
  }

  case object Kodak262 extends CompressionValue {
    val value        = 262
    val displayValue = "Kodak 262"
  }

  case object Next extends CompressionValue {
    val value        = 32766
    val displayValue = "Next"
  }

  case object SonyARWCompressed extends CompressionValue {
    val value        = 32767
    val displayValue = "Sony ARW Compressed"
  }

  case object PackedRAW extends CompressionValue {
    val value        = 32769
    val displayValue = "Packed RAW"
  }

  case object SamsungSRWCompressed extends CompressionValue {
    val value        = 32770
    val displayValue = "Samsung SRW Compressed"
  }

  case object CCIRLEW extends CompressionValue {
    val value        = 32771
    val displayValue = "CCIRLEW"
  }

  case object SamsungSRWCompressed2 extends CompressionValue {
    val value        = 32772
    val displayValue = "Samsung SRW Compressed 2"
  }

  case object PackBits extends CompressionValue {
    val value        = 32773
    val displayValue = "PackBits"
  }

  case object Thunderscan extends CompressionValue {
    val value        = 32809
    val displayValue = "Thunderscan"
  }

  case object KodakKDCCompressed extends CompressionValue {
    val value        = 32867
    val displayValue = "KodakKDCCompressed"
  }

  case object IT8CTPAD extends CompressionValue {
    val value        = 32895
    val displayValue = "IT8CTPAD"
  }

  case object IT8LW extends CompressionValue {
    val value        = 32896
    val displayValue = "IT8LW"
  }

  case object IT8MP extends CompressionValue {
    val value        = 32897
    val displayValue = "IT8MP"
  }

  case object IT8BL extends CompressionValue {
    val value        = 32898
    val displayValue = "IT8BL"
  }

  case object PixarFilm extends CompressionValue {
    val value        = 32908
    val displayValue = "PixarFilm"
  }

  case object PixarLog extends CompressionValue {
    val value        = 32909
    val displayValue = "PixarLog"
  }

  case object Deflate extends CompressionValue {
    val value        = 32946
    val displayValue = "Deflate"
  }

  case object DCS extends CompressionValue {
    val value        = 32947
    val displayValue = "DCS"
  }

  case object AperioJPEG2000YCbCr extends CompressionValue {
    val value        = 33003
    val displayValue = "Aperio JPEG 2000 YCbCr"
  }

  case object AperioJPEG2000RGB extends CompressionValue {
    val value        = 33005
    val displayValue = "Aperio JPEG 2000 RGB"
  }

  case object JBIG extends CompressionValue {
    val value        = 34661
    val displayValue = "JBIG"
  }

  case object SGILog extends CompressionValue {
    val value        = 34676
    val displayValue = "SGILog"
  }

  case object SGILog24 extends CompressionValue {
    val value        = 34677
    val displayValue = "SGILog24"
  }

  case object JPEG2000 extends CompressionValue {
    val value        = 34712
    val displayValue = "JPEG 2000"
  }

  case object NikonNEFCompressed extends CompressionValue {
    val value        = 34713
    val displayValue = "NikonNEFCompressed"
  }

  case object JBIG2TIFFFX extends CompressionValue {
    val value        = 34715
    val displayValue = "JBIG2 TIFF FX"
  }

  case object MDIBinaryLevelCodec extends CompressionValue {
    val value        = 34718
    val displayValue = "Microsoft Document Imaging (MDI) Binary Level Codec"
  }

  case object MDIProgressiveTransformCodec extends CompressionValue {
    val value        = 34719
    val displayValue = "Microsoft Document Imaging (MDI) Progressive Transform Codec"
  }

  case object MDIVector extends CompressionValue {
    val value        = 34720
    val displayValue = "Microsoft Document Imaging (MDI) Vector"
  }

  case object ESRILerc extends CompressionValue {
    val value        = 34887
    val displayValue = "ESRILerc"
  }

  case object LossyJPEG extends CompressionValue {
    val value        = 34892
    val displayValue = "LossyJPEG"
  }

  case object LZMA2 extends CompressionValue {
    val value        = 34925
    val displayValue = "LZMA2"
  }

  case object Zstd extends CompressionValue {
    val value        = 34926
    val displayValue = "Zstd"
  }

  case object WebP extends CompressionValue {
    val value        = 34927
    val displayValue = "WebP"
  }

  case object PNG extends CompressionValue {
    val value        = 34933
    val displayValue = "PNG"
  }

  case object JPEGXR extends CompressionValue {
    val value        = 34934
    val displayValue = "JPEGXR"
  }

  case object KodakDCRCompressed extends CompressionValue {
    val value        = 65000
    val displayValue = "Kodak DCR Compressed"
  }

  case object PentaxPEFCompressed extends CompressionValue {
    val value        = 65535
    val displayValue = "Pentax PEF Compressed"
  }

  /**
    * Find the enum by its integer value
    * Return None if the value is not found
    * */
  private final val listCompression: List[CompressionValue] = List(
    Uncompressed,
    CCITT1D,
    T4Group3Fax,
    T6Group4Fax,
    LZW,
    JPEGOldStyle,
    JPEG1,
    AdobeDeflate,
    JBIGBAndW,
    JBIGColor,
    JPEG2,
    Kodak262,
    Next,
    SonyARWCompressed,
    PackedRAW,
    SamsungSRWCompressed,
    CCIRLEW,
    SamsungSRWCompressed2,
    PackBits,
    Thunderscan,
    KodakKDCCompressed,
    IT8CTPAD,
    IT8LW,
    IT8MP,
    IT8BL,
    PixarFilm,
    PixarLog,
    Deflate,
    DCS,
    AperioJPEG2000YCbCr,
    AperioJPEG2000RGB,
    JBIG,
    SGILog,
    SGILog24,
    JPEG2000,
    NikonNEFCompressed,
    JBIG2TIFFFX,
    MDIBinaryLevelCodec,
    MDIProgressiveTransformCodec,
    MDIVector,
    ESRILerc,
    LossyJPEG,
    LZMA2,
    Zstd,
    WebP,
    PNG,
    JPEGXR,
    KodakDCRCompressed,
    PentaxPEFCompressed
  )
  def getMetadataValue(value: Int): Option[CompressionValue] = listCompression.find(_.value.equals(value))
}

case class ResolutionUnit(unit: String) extends MetadataValue {
  val displayValue = "Resolution Unit"
}

object ResolutionUnit extends MetadataJsonFields with ReadableValue {
  implicit val resolutionUnitReads: Reads[ResolutionUnit] =
    (__ \ resolutionUnitField).read[String].map(ResolutionUnit.apply)

  implicit val resolutionUnitWrites: Writes[ResolutionUnit] = writesBuilder(
    (data: ResolutionUnit) => (data.displayValue, fromString(data.unit))
  )

  case object Inches extends MetadataValue {
    val value        = 2
    val displayValue = "inches"
  }
  private val someInches = Some(Inches)

  case object Cm extends MetadataValue {
    val value        = 3
    val displayValue = "cm"
  }
  private val someCm = Some(Cm)

  /**
    * Find the enum by its integer value
    * Return None if the value is not found
    * */
  def getMetadataValue(value: Int): Option[MetadataValue] = {
    value match {
      case Inches.value => someInches
      case Cm.value     => someCm
      case _            => None
    }
  }
}

case class ColorSpace(colorSpace: String) extends MetadataValue {
  val displayValue = "Color Representation"
}

object ColorSpace extends MetadataJsonFields with ReadableValue {
  implicit val colorSpaceReads: Reads[ColorSpace] =
    (__ \ colorSpaceField).read[String].map(ColorSpace.apply)

  implicit val colorSpaceWrites: Writes[ColorSpace] = writesBuilder(
    (data: ColorSpace) => (data.displayValue, fromString(data.colorSpace))
  )

  case object SRBG extends MetadataValue {
    val value        = 0x1
    val displayValue = "sRGB"
  }
  private val someSRBG = Some(SRBG)

  case object Uncalibrated extends MetadataValue {
    val value        = 0xffff
    val displayValue = "Uncalibrated"
  }
  private val someUncalibrated = Some(Uncalibrated)

  /**
    * Find the enum by its integer value
    * Return None if the value is not found
    * */
  def getMetadataValue(value: Int): Option[MetadataValue] = {
    value match {
      case SRBG.value         => someSRBG
      case Uncalibrated.value => someUncalibrated
      case _                  => None
    }
  }
}

case class CompressedBitsPerPixel(c: String) extends MetadataValue {
  val displayValue = "Compressed Bits/Pixel"
}

object CompressedBitsPerPixel extends MetadataJsonFields {
  implicit val compressedBitsPerPixelReads: Reads[CompressedBitsPerPixel] =
    (__ \ compressedBitsPerPixelField).read[String].map(CompressedBitsPerPixel.apply)

  implicit val compressedBitsPerPixelWrites: Writes[CompressedBitsPerPixel] = writesBuilder(
    (data: CompressedBitsPerPixel) => (data.displayValue, data.c))
}

case class Make(make: String) extends MetadataValue {
  val displayValue = "Camera Make"
}

object Make extends MetadataJsonFields {
  implicit val makeReads: Reads[Make] =
    (__ \ makeField).read[String].map(Make.apply)

  implicit val makeWrites: Writes[Make] = writesBuilder(
    (data: Make) => (data.displayValue, data.make)
  )
}

case class Model(model: String) extends MetadataValue {
  val displayValue = "Camera Model"
}

object Model extends MetadataJsonFields {
  implicit val modelReads: Reads[Model] = (__ \ modelField).read[String].map(Model.apply)

  implicit val modelWrites: Writes[Model] = writesBuilder((data: Model) => (data.displayValue, data.model))
}

case class BodySerialNumber(serial: String) extends MetadataValue {
  val displayValue = "Camera Serial Number"
}

object BodySerialNumber extends MetadataJsonFields {
  implicit val bodySerialNumberReads: Reads[BodySerialNumber] =
    (__ \ bodySerialNumberField).read[String].map(BodySerialNumber.apply)

  implicit val bodySerialNumberWrites: Writes[BodySerialNumber] = writesBuilder(
    (data: BodySerialNumber) => (data.displayValue, data.serial))
}

case class FNumber(fNumber: String) extends MetadataValue {
  val displayValue = "F-Stop"
}

object FNumber extends MetadataJsonFields {
  implicit val fNumberReads: Reads[FNumber] = (__ \ fNumberField).read[String].map(FNumber.apply)

  implicit val fNumberWrites: Writes[FNumber] = writesBuilder((data: FNumber) => (data.displayValue, data.fNumber))
}

case class ExposureTime(exposureTime: String) extends MetadataValue {
  val displayValue = "Exposure Time"
}

object ExposureTime extends MetadataJsonFields {
  implicit val ExposureTimeReads: Reads[ExposureTime] = (__ \ exposureTimeField).read[String].map(ExposureTime.apply)
  implicit val ExposureTimeWrites: Writes[ExposureTime] = writesBuilder(
    (data: ExposureTime) => (data.displayValue, data.exposureTime))
}

case class ShutterSpeedValue(ssv: String) extends MetadataValue {
  val displayValue = "Shutter Speed"
}

object ShutterSpeedValue extends MetadataJsonFields {
  implicit val ShutterSpeedValueReads: Reads[ShutterSpeedValue] =
    (__ \ shutterSpeedValueField).read[String].map(ShutterSpeedValue.apply)
  implicit val ShutterSpeedValueWrites: Writes[ShutterSpeedValue] = writesBuilder(
    (data: ShutterSpeedValue) => (data.displayValue, data.ssv))
}

case class ISOSpeedRatings(iso: String) extends MetadataValue {
  val displayValue = "ISO Speed"
}

object ISOSpeedRatings extends MetadataJsonFields {
  implicit val iSOSpeedRatingsReads: Reads[ISOSpeedRatings] =
    (__ \ isoSpeedRatingsField).read[String].map(ISOSpeedRatings.apply)

  implicit val iSOSpeedRatingsWrites: Writes[ISOSpeedRatings] = writesBuilder(
    (data: ISOSpeedRatings) => (data.displayValue, data.iso))
}

case class ExposureBiasValue(value: String) extends MetadataValue {
  val displayValue = "Exposure Bias"
}

object ExposureBiasValue extends MetadataJsonFields {
  implicit val exposureBiasValueReads: Reads[ExposureBiasValue] =
    (__ \ exposureBiasValueField).read[String].map(ExposureBiasValue.apply)

  implicit val exposureBiasValueWrites: Writes[ExposureBiasValue] = writesBuilder(
    (data: ExposureBiasValue) => (data.displayValue, data.value))
}

case class FocalLength(focalLength: String) extends MetadataValue {
  val displayValue = "Focal Length"
}

object FocalLength extends MetadataJsonFields {
  implicit val focalLengthReads: Reads[FocalLength] = (__ \ focalLengthField).read[String].map(FocalLength.apply)

  implicit val focalWrites: Writes[FocalLength] = writesBuilder(
    (data: FocalLength) => (data.displayValue, data.focalLength))
}

case class FocalLengthIn35mmFilm(focalLength35mm: String) extends MetadataValue {
  val displayValue = "35mm Focal Length"
}

object FocalLengthIn35mmFilm extends MetadataJsonFields {
  implicit val focalLengthIn35mmFilmReads: Reads[FocalLengthIn35mmFilm] =
    (__ \ focalLengthIn35mmFilmField).read[String].map(FocalLengthIn35mmFilm.apply)

  implicit val focalLengthIn35mmFilmWrites: Writes[FocalLengthIn35mmFilm] = writesBuilder(
    (data: FocalLengthIn35mmFilm) => (data.displayValue, data.focalLength35mm))
}

case class ApertureValue(value: String) extends MetadataValue {
  val displayValue = "Aperture Value"
}

object ApertureValue extends MetadataJsonFields {
  implicit val apertureValueReads: Reads[ApertureValue] =
    (__ \ apertureValueField).read[String].map(ApertureValue.apply)

  implicit val apertureValueWrites: Writes[ApertureValue] = writesBuilder(
    (data: ApertureValue) => (data.displayValue, data.value))
}

case class MaxApertureValue(value: String) extends MetadataValue {
  val displayValue = "Max Aperture Value"
}

object MaxApertureValue extends MetadataJsonFields {
  implicit val maxApertureValueReads: Reads[MaxApertureValue] =
    (__ \ maxApertureValueField).read[String].map(MaxApertureValue.apply)

  implicit val maxApertureValueWrites: Writes[MaxApertureValue] = writesBuilder(
    (data: MaxApertureValue) => (data.displayValue, data.value))
}

case class MeteringMode(mMode: String) extends MetadataValue {
  val displayValue = "Metering Mode"
}

object MeteringMode extends MetadataJsonFields with ReadableValue {
  implicit val meteringModeReads: Reads[MeteringMode] = (__ \ meteringModeField).read[String].map(MeteringMode.apply)

  implicit val meteringModeWrites: Writes[MeteringMode] = writesBuilder(
    (data: MeteringMode) => (data.displayValue, fromString(data.mMode)))

  case object Unknown extends MetadataValue {
    val value        = 0
    val displayValue = "Unknown"
  }
  private val someUnknown = Some(Unknown)

  case object Average extends MetadataValue {
    val value        = 1
    val displayValue = "Average"
  }
  private val someAverage = Some(Average)

  case object CenterWeightedAverage extends MetadataValue {
    val value        = 2
    val displayValue = "Center-weighted average"
  }
  private val someCenterWeightedAverage = Some(CenterWeightedAverage)

  case object Spot extends MetadataValue {
    val value        = 3
    val displayValue = "Spot"
  }
  private val someSpot = Some(Spot)

  case object MultiSpot extends MetadataValue {
    val value        = 4
    val displayValue = "Multi-spot"
  }
  private val someMultiSpot = Some(MultiSpot)

  case object MultiSegment extends MetadataValue {
    val value        = 5
    val displayValue = "Multi-segment"
  }
  private val someMultiSegment = Some(MultiSegment)

  case object Partial extends MetadataValue {
    val value        = 6
    val displayValue = "Partial"
  }
  private val somePartial = Some(Partial)

  case object Other extends MetadataValue {
    val value        = 255
    val displayValue = "Other"
  }
  private val someOther = Some(Other)

  /**
    * Find the enum by its integer value
    * Return None if the value is not found
    * */
  def getMetadataValue(value: Int): Option[MetadataValue] =
    value match {
      case Unknown.value               => someUnknown
      case Average.value               => someAverage
      case CenterWeightedAverage.value => someCenterWeightedAverage
      case Spot.value                  => someSpot
      case MultiSpot.value             => someMultiSpot
      case MultiSegment.value          => someMultiSegment
      case Partial.value               => somePartial
      case Other.value                 => someOther
      case _                           => None
    }
}

case class SubjectDistance(sDistance: String) extends MetadataValue {
  val displayValue = "Subject Distance"
}

object SubjectDistance extends MetadataJsonFields {
  implicit val subjectDistanceReads: Reads[SubjectDistance] =
    (__ \ subjectDistanceField).read[String].map(SubjectDistance.apply)

  implicit val subjectDistanceWrites: Writes[SubjectDistance] = writesBuilder(
    (data: SubjectDistance) => (data.displayValue, data.sDistance))
}

case class Flash(flash: String) extends MetadataValue {
  val displayValue = "Flash Mode"
}

object Flash extends MetadataJsonFields with ReadableValue {
  implicit val flashReads: Reads[Flash] = (__ \ flashField).read[String].map(Flash.apply)

  implicit val flashWrites: Writes[Flash] = writesBuilder(
    (data: Flash) => (data.displayValue, fromString(data.flash))
  )

  case object NoFlash extends MetadataValue {
    val value        = 0x0
    val displayValue = "No Flash"
  }
  private val someNoFlash = Some(NoFlash)

  case object Fired extends MetadataValue {
    val value        = 0x1
    val displayValue = "Fired"
  }
  private val someFired = Some(Fired)

  case object FiredReturnNotDetected extends MetadataValue {
    val value        = 0x5
    val displayValue = "Fired, Return not detected"
  }
  private val someFiredReturnNotDetected = Some(FiredReturnNotDetected)

  case object FiredReturnDetected extends MetadataValue {
    val value        = 0x7
    val displayValue = "Fired, Return detected"
  }
  private val someFiredReturnDetected = Some(FiredReturnDetected)

  case object OnDidNotFire extends MetadataValue {
    val value        = 0x8
    val displayValue = "On, Did not fire"
  }
  private val someOnDidNotFire = Some(OnDidNotFire)

  case object OnFired extends MetadataValue {
    val value        = 0x9
    val displayValue = "On, Fired"
  }
  private val someOnFired = Some(OnFired)

  case object OnReturnNotDetected extends MetadataValue {
    val value        = 0xd
    val displayValue = " On, Return not detected"
  }
  private val someOnReturnNotDetected = Some(OnReturnNotDetected)

  case object OnReturnDetected extends MetadataValue {
    val value        = 0xf
    val displayValue = "On, Return detected"
  }
  private val someOnReturnDetected = Some(OnReturnDetected)

  case object OffDidNotFire extends MetadataValue {
    val value        = 0x10
    val displayValue = "Off, Did not fire"
  }
  private val someOffDidNotFire = Some(OffDidNotFire)

  case object OffDidNotFireReturnNotDetected extends MetadataValue {
    val value        = 0x14
    val displayValue = "Off, Did not fire, Return not detected"
  }
  private val someOffDidNotFireReturnNotDetected = Some(OffDidNotFireReturnNotDetected)

  case object AutoDidNotFire extends MetadataValue {
    val value        = 0x18
    val displayValue = "Auto, Did not fire"
  }
  private val someAutoDidNotFire = Some(AutoDidNotFire)

  case object AutoFired extends MetadataValue {
    val value        = 0x19
    val displayValue = "Auto, Fired"
  }
  private val someAutoFired = Some(AutoFired)

  case object AutoFiredReturnNotDetected extends MetadataValue {
    val value        = 0x1d
    val displayValue = "Auto, Fired, Return not detected"
  }
  private val someAutoFiredReturnNotDetected = Some(AutoFiredReturnNotDetected)

  case object AutoFiredReturnDetected extends MetadataValue {
    val value        = 0x1f
    val displayValue = "Auto, Fired, Return detected"
  }
  private val someAutoFiredReturnDetected = Some(AutoFiredReturnDetected)

  case object NoFlashFunction extends MetadataValue {
    val value        = 0x20
    val displayValue = "No flash function"
  }
  private val someNoFlashFunction = Some(NoFlashFunction)

  case object OffNoFlashFunction extends MetadataValue {
    val value        = 0x30
    val displayValue = "Off, No flash function"
  }
  private val someOffNoFlashFunction = Some(OffNoFlashFunction)

  case object FiredRedEyeReduction extends MetadataValue {
    val value        = 0x41
    val displayValue = "Fired, Red-eye reduction"
  }
  private val someFiredRedEyeReduction = Some(FiredRedEyeReduction)

  case object FiredRedEyeReductionReturnNotDetected extends MetadataValue {
    val value        = 0x45
    val displayValue = "Fired, Red-eye reduction, Return not detected"
  }
  private val someFiredRedEyeReductionReturnNotDetected = Some(FiredRedEyeReductionReturnNotDetected)

  case object FiredRedEyeReductionReturnDetected extends MetadataValue {
    val value        = 0x47
    val displayValue = "Fired, Red-eye reduction, Return detected"
  }
  private val someFiredRedEyeReductionReturnDetected = Some(FiredRedEyeReductionReturnDetected)

  case object OnRedEyeReduction extends MetadataValue {
    val value        = 0x49
    val displayValue = "On, Red-eye reduction"
  }
  private val someOnRedEyeReduction = Some(OnRedEyeReduction)

  case object OnRedEyeReductionReturnNotDetected extends MetadataValue {
    val value        = 0x4d
    val displayValue = "On, Red-eye reduction, Return not detected"
  }
  private val someOnRedEyeReductionReturnNotDetected = Some(OnRedEyeReductionReturnNotDetected)

  case object OnRedEyeReductionReturnDetected extends MetadataValue {
    val value        = 0x4f
    val displayValue = "On, Red-eye reduction, Return detected"
  }
  private val someOnRedEyeReductionReturnDetected = Some(OnRedEyeReductionReturnDetected)

  case object OffRedEyeReduction extends MetadataValue {
    val value        = 0x50
    val displayValue = "Off, Red-eye reduction"
  }
  private val someOffRedEyeReduction = Some(OffRedEyeReduction)

  case object AutoDidNotFireRedEyeReduction extends MetadataValue {
    val value        = 0x58
    val displayValue = "Auto, Did not fire, Red-eye reduction"
  }
  private val someAutoDidNotFireRedEyeReduction = Some(AutoDidNotFireRedEyeReduction)

  case object AutoFiredRedEyeReduction extends MetadataValue {
    val value        = 0x59
    val displayValue = "Auto, Fired, Red-eye reduction"
  }
  private val someAutoFiredRedEyeReduction = Some(AutoFiredRedEyeReduction)

  case object AutoFiredRedEyeReductionReturnNotDetected extends MetadataValue {
    val value        = 0x5d
    val displayValue = "Auto, Fired, Red-eye reduction, Return not detected"
  }
  private val someAutoFiredRedEyeReductionReturnNotDetected = Some(AutoFiredRedEyeReductionReturnNotDetected)

  case object AutoFiredRedEyeReductionReturnDetected extends MetadataValue {
    val value        = 0x5f
    val displayValue = "Auto, Fired, Red-eye reduction, Return detected"
  }
  private val someAutoFiredRedEyeReductionReturnDetected = Some(AutoFiredRedEyeReductionReturnDetected)

  /**
    * Find the enum by its integer value
    * Return None if the value is not found
    * */
  def getMetadataValue(value: Int): Option[MetadataValue] =
    value match {
      case NoFlash.value                                   => someNoFlash
      case Fired.value                                     => someFired
      case FiredReturnNotDetected.value                    => someFiredReturnNotDetected
      case FiredReturnDetected.value                       => someFiredReturnDetected
      case OnDidNotFire.value                              => someOnDidNotFire
      case OnFired.value                                   => someOnFired
      case OnReturnNotDetected.value                       => someOnReturnNotDetected
      case OnReturnDetected.value                          => someOnReturnDetected
      case OffDidNotFire.value                             => someOffDidNotFire
      case OffDidNotFireReturnNotDetected.value            => someOffDidNotFireReturnNotDetected
      case AutoDidNotFire.value                            => someAutoDidNotFire
      case AutoFired.value                                 => someAutoFired
      case AutoFiredReturnNotDetected.value                => someAutoFiredReturnNotDetected
      case AutoFiredReturnDetected.value                   => someAutoFiredReturnDetected
      case NoFlashFunction.value                           => someNoFlashFunction
      case OffNoFlashFunction.value                        => someOffNoFlashFunction
      case FiredRedEyeReduction.value                      => someFiredRedEyeReduction
      case FiredRedEyeReductionReturnNotDetected.value     => someFiredRedEyeReductionReturnNotDetected
      case FiredRedEyeReductionReturnDetected.value        => someFiredRedEyeReductionReturnDetected
      case OnRedEyeReduction.value                         => someOnRedEyeReduction
      case OnRedEyeReductionReturnNotDetected.value        => someOnRedEyeReductionReturnNotDetected
      case OnRedEyeReductionReturnDetected.value           => someOnRedEyeReductionReturnDetected
      case OffRedEyeReduction.value                        => someOffRedEyeReduction
      case AutoDidNotFireRedEyeReduction.value             => someAutoDidNotFireRedEyeReduction
      case AutoFiredRedEyeReduction.value                  => someAutoFiredRedEyeReduction
      case AutoFiredRedEyeReductionReturnNotDetected.value => someAutoFiredRedEyeReductionReturnNotDetected
      case AutoFiredRedEyeReductionReturnDetected.value    => someAutoFiredRedEyeReductionReturnDetected
      case _                                               => None
    }
}

case class DateTimeOriginal(dto: String) extends MetadataValue {
  val displayValue = "Date/Time Original"
}

object DateTimeOriginal extends MetadataJsonFields {
  implicit val dateTimeOriginalReads: Reads[DateTimeOriginal] =
    (__ \ dateTimeOriginalField).read[String].map(DateTimeOriginal.apply)

  implicit val dateTimeOriginalWrites: Writes[DateTimeOriginal] = writesBuilder(
    (data: DateTimeOriginal) => (data.displayValue, data.dto))
}

case class DateTimeDigitized(dtd: String) extends MetadataValue {
  val displayValue = "Date/Time Original"
}

object DateTimeDigitized extends MetadataJsonFields {
  implicit val dateTimeDigitizedReads: Reads[DateTimeDigitized] =
    (__ \ dateTimeDigitizedField).read[String].map(DateTimeDigitized.apply)

  implicit val dateTimeOriginalWrites: Writes[DateTimeDigitized] = writesBuilder(
    (data: DateTimeDigitized) => (data.displayValue, data.dtd))
}
