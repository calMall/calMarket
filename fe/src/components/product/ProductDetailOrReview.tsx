"use client";
import { useState } from "react";
import ReviewContain from "../review/ReviewContain";

interface props {
  itemCode: string;
  text: string;
  rating: number;
}
export default function ProductDetailOrReview({
  itemCode,
  text,
  rating,
}: props) {
  const [type, setType] = useState<"detail" | "review">("detail");

  return (
    <div className="mt-1">
      <div className="wf product-detail-select-contain">
        <div className="rt">
          <button
            className={`${type === "detail" ? "selected" : ""}`}
            onClick={() => setType("detail")}
          >
            商品詳細
          </button>
          <button
            className={`${type === "review" ? "selected" : ""}`}
            onClick={() => setType("review")}
          >
            レビュー
          </button>
          <div
            className={`ab ${type === "detail" ? "horizon" : "horizon-right"}`}
          />
        </div>
      </div>
      {type === "detail" ? (
        <div className="mt-1 whitespace-pre-line break-words">{text}</div>
      ) : (
        <div>
          <ReviewContain rating={rating} itemCode={itemCode} />
        </div>
      )}
    </div>
  );
}
