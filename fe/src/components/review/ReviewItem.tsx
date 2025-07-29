import { dateFormat } from "@/utils/dateFormat";
import Star from "../product/Star";
import ContainImage from "../common/ContainImage";

export default function ReviewItem({ review }: { review: ReviewDTOonProduct }) {
  return (
    <div className="review-item wf">
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
    </div>
  );
}
