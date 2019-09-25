package services.rti.metadata

import play.api.libs.json.JsValue

trait MetadataJsonConversions {

  def metadataFromJson(json: JsValue): Metadata = {
    Metadata(
      json.asOpt[DateTime],
      json.asOpt[ImageDescription],
      json.asOpt[Software],
      json.asOpt[Orientation],
      json.asOpt[PixelXDimension],
      json.asOpt[PixelYDimension],
      json.asOpt[XResolution],
      json.asOpt[YResolution],
      json.asOpt[Compression],
      json.asOpt[ResolutionUnit],
      json.asOpt[ColorSpace],
      json.asOpt[CompressedBitsPerPixel],
      json.asOpt[Make],
      json.asOpt[Model],
      json.asOpt[BodySerialNumber],
      json.asOpt[FNumber],
      json.asOpt[ExposureTime],
      json.asOpt[ShutterSpeedValue],
      json.asOpt[ISOSpeedRatings],
      json.asOpt[ExposureBiasValue],
      json.asOpt[FocalLength],
      json.asOpt[FocalLengthIn35mmFilm],
      json.asOpt[ApertureValue],
      json.asOpt[MaxApertureValue],
      json.asOpt[MeteringMode],
      json.asOpt[SubjectDistance],
      json.asOpt[Flash],
      json.asOpt[DateTimeOriginal],
      json.asOpt[DateTimeDigitized],
      json.asOpt[LensMake],
      json.asOpt[LensModel],
      json.asOpt[LensSerialNumber],
      json.asOpt[LensSpecification],
      json.asOpt[ComponentsConfiguration],
      json.asOpt[Contrast],
      json.asOpt[BrightnessValue],
      json.asOpt[LightSource],
      json.asOpt[ExposureProgram],
      json.asOpt[ExposureMode],
      json.asOpt[Saturation],
      json.asOpt[Sharpness],
      json.asOpt[WhiteBalance],
      json.asOpt[DigitalZoomRatio],
      json.asOpt[SceneType],
      json.asOpt[SceneCaptureType],
      json.asOpt[FileSource],
      json.asOpt[ExifVersion],
      json.asOpt[FlashpixVersion]
    )
  }

}
