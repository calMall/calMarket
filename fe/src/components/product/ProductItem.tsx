import { rakutenApiItem } from "@/types/RakutenAPI";
import CoverImage from "../common/CoverImage";
import Star from "./Star";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";

interface props {
  product: rakutenApiItem;
}
export default function ProductItem({ product }: props) {
  return (
    <Link href={"/"} className="product-list-item-contain">
      <div className="rt product-list-item-img">
        <CoverImage
          url={newImageSizing(product.Item.mediumImageUrls[0].imageUrl, 256)}
          alt="img"
        />
      </div>
      <div className="product-list-item-name">{product.Item.itemName}</div>
      <Star score={Number(product.Item.reviewAverage)} />
      <div className="rating">
        楽天レビュー
        <span className="fw-500">
          {product.Item.reviewCount.toLocaleString()}件
        </span>
      </div>
      <div className="fw-500 list-price mt-05">
        {Number(product.Item.itemPrice).toLocaleString()}円
      </div>
    </Link>
  );
}
