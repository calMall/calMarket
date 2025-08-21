import ProductDetailCartBtns from "./ProductDetailCartBtns";
import Star from "./Star";

interface props {
  itemName: string;
  rating: number;
  price: number;
  explanation: string;
  code: string;
  reviewCnt: number;
}

export default function ProductDetailTitle({
  itemName,
  rating,
  price,
  explanation,
  reviewCnt,
  code,
}: props) {
  return (
    <div>
      <h3 className="m-none">{itemName}</h3>
      <div className="flex ae gap-05 mt-1">
        <Star className="detail-" score={rating} />
        <div className="rating fw-500">{reviewCnt}件の評価</div>
      </div>
      <div className="detail-price mt-1">{price.toLocaleString()}円</div>
      <div className="mt-1">{explanation}</div>
      <ProductDetailCartBtns itemCode={code} />
    </div>
  );
}
