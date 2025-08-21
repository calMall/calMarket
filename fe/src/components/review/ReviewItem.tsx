import { dateFormat } from "@/utils/dateFormat";
import Star from "../product/Star";
import ContainImage from "../common/ContainImage";
import { useState } from "react";
import { postReviewLike } from "@/api/Review";
import UseUnauthorized from "@/hooks/UseUnauthorized";
import { AiFillLike, AiOutlineLike } from "react-icons/ai";
import ReviewImageModal from "./ReviewImageModal";
import ReviewMenu from "./ReviewMenu";

export default function ReviewItem({
  review,
  isMy,
  refreshData,
  itemCode,
}: {
  review: ReviewDTOonProduct;
  itemCode: string;
  isMy?: boolean;
  refreshData: Function;
}) {
  const [isLike, setIsLike] = useState(review.like);
  const [likeCount, setLikeCount] = useState(review.likeCount);
  const [isViewModal, setIsViewModal] = useState(false);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const handleImageClick = (imageUrl: string) => {
    setIsViewModal(true);
    setSelectedImage(imageUrl);
  };
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
      <div className="flex ac jb">
        <div className="flex ac gap-05">
          <div className="profile-img rt">
            <ContainImage url="/profile.png" alt="profile" />
          </div>
          <span>{review.userNickname}</span>
        </div>
        <ReviewMenu
          itemCode={itemCode}
          refreshData={refreshData}
          reviewId={review.reviewId}
        />
      </div>
      <Star score={review.rating} />
      <div className="date-font">{dateFormat(review.createdAt)}</div>
      <div className="review-title-in-product mt-05">{review.title}</div>
      <div>
        {review.imageList && review.imageList.length > 0 && (
          <div className="review-image-contain mt-1 gap-05 flex ac">
            {review.imageList.map((url, idx) => (
              <button
                className="review-image rt"
                key={idx}
                onClick={() => handleImageClick(url)}
              >
                <ContainImage url={`${url}`} alt={`review-image-${idx}`} />
              </button>
            ))}
          </div>
        )}
        {isViewModal && selectedImage && (
          <ReviewImageModal
            setViewModal={setIsViewModal}
            imageUrl={selectedImage}
          />
        )}
      </div>
      <div className="mt-1">{review.comment}</div>
      <div className="mt-1 like-comment">
        {likeCount}人のお客様がこれが役に立ったと考えています
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
