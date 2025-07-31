import { dateFormat } from "@/utils/dateFormat";
import Star from "../product/Star";
import ContainImage from "../common/ContainImage";
import { useState } from "react";
import { postReviewLike } from "@/api/Review";
import UseUnauthorized from "@/hooks/UseUnauthorized";
import { AiFillLike, AiOutlineLike } from "react-icons/ai";

export default function ReviewItem({
  review,
  isMy,
}: {
  review: ReviewDTOonProduct;
  isMy?: boolean;
}) {
  const [isLike, setIsLike] = useState(review.like);
  const [likeCount, setLikeCount] = useState(review.likeCount);
  const useUnauthorized = UseUnauthorized();
  const toggleLike = async () => {
    try {
      const res = await postReviewLike(review.reviewId);
      if (res.message === "success") {
        setIsLike(!isLike);
        setLikeCount(isLike ? likeCount - 1 : likeCount + 1);
      } else {
        alert("レビューのいいねに失敗しました。");
      }
    } catch (e: any) {
      if (e.status === 401) {
        return useUnauthorized();
      }
      return alert("レビューのいいね中にエラーが発生しました。");
    }
  };

  return (
    <div className={`review-item wf ${isMy ? "my-review" : ""}`}>
      <div className="flex ac gap-05">
        <div className="profile-img rt">
          <ContainImage url="/profile.png" alt="profile" />
        </div>
        <span>{review.userNickname}</span>
      </div>
      <Star score={review.rating} />
      <div className="date-font">{dateFormat(review.createdAt)}</div>
      <div className="review-title-in-product mt-05">{review.title}</div>
      <div className="mt-1">{review.comment}</div>
      <div className="mt-1 like-comment">
        {review.likeCount}人のお客様がこれが役に立ったと考えています
      </div>
      <button
        onClick={toggleLike}
        className={`btn-like flex ac gap-05 ${isLike ? "btn-liked" : ""}`}
      >
        {!isLike ? (
          <>
            <AiOutlineLike /> 役に立った
          </>
        ) : (
          <>
            <AiFillLike />
            キャンセル
          </>
        )}
      </button>
    </div>
  );
}
