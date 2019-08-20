package services.rti

sealed trait SizeImage {
  def value: String
}

case object SmallImage  extends SizeImage { val value = "small"  }
case object MediumImage extends SizeImage { val value = "medium" }
case object LargeImage  extends SizeImage { val value = "large"  }

sealed trait QualityImage { def value: String }

object Quality {
  case object LowQuality    extends QualityImage { val value = "low"    }
  case object MediumQuality extends QualityImage { val value = "medium" }
  case object HighQuality   extends QualityImage { val value = "high"   }
}
