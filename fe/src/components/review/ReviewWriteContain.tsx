"use client";

import { useState } from "react";
import CustomButton from "../common/CustomBtn";
import { postReview } from "@/api/Review";
interface props {
  itemCode: string;
}
export default function ReviewWriteContain({ itemCode }: props) {
  const [hovered, setHovered] = useState(0);
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState("");
  const decodedItemCode = decodeURIComponent(itemCode as string);
  const onPostReview = async () => {
    if (!rating) return alert("レビューには星の評価が必要です。");
    if (!content) return alert("レビュー内容を入力してください。");
    const review: ReviewRequestDto = {
      itemCode: decodedItemCode,
      rating,
      comment: content,
      title: "asd",
      imageList: [],
    };
    try {
      const res = await postReview(review);
      if (res.message === "success") {
        alert("レビューが投稿されました。");
      } else {
        alert("レビューの投稿に失敗しました。");
      }
    } catch (e) {
      console.log(e);
      alert("レビューの投稿中にエラーが発生しました。");
    }
  };
  return (
    <div className="mt-2">
      <div style={{ display: "flex", gap: "4px" }}>
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            style={{
              cursor: "pointer",
              fontSize: "2.5rem",
              color: (hovered || rating) >= star ? "#ffc107" : "#e4e5e9",
            }}
            onMouseEnter={() => setHovered(star)}
            onMouseLeave={() => setHovered(0)}
            onClick={() => setRating(star)}
          >
            ★
          </span>
        ))}
      </div>
      <h4>レビューを書く</h4>
      <div>
        <textarea
          placeholder="他のお客様が知っておくべきことは何ですか？"
          className="wf pd-1 border cart-border bx"
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
      </div>
      <div className="wf flex je ">
        <CustomButton
          func={onPostReview}
          text="投稿"
          classname="review-post-btn"
        />
      </div>
    </div>
  );
}
