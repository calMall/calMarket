"use client";

import { deleteReview } from "@/api/Review";
import UseUnauthorized from "@/hooks/UseUnauthorized";
import Link from "next/link";
import { useRouter } from "next/navigation";

interface props {
  itemCode: string;
  reviewId: number;
}
export default function ReviewDetailBtns({ itemCode, reviewId }: props) {
  const useUnauthorized = UseUnauthorized;
  const router = useRouter();
  const onDelete = async () => {
    if (window.confirm("レビューを削除しますか？")) {
      try {
        const res = await deleteReview(reviewId);
        if (res.message === "success") {
          alert("レビューを削除しました。");
          return router.back();
        }
      } catch (e: any) {
        if (e.status === 401) {
          return useUnauthorized();
        }
        return alert("レビューのいいね中にエラーが発生しました。");
      }
    }
  };
  return (
    <div className="wf flex gap-1 je">
      <Link
        href={`/review/write/${itemCode}?reviewId=${reviewId}`}
        className="review-detail-btns"
      >
        編集
      </Link>
      <button onClick={onDelete} className="review-detail-delete">
        削除
      </button>
    </div>
  );
}
