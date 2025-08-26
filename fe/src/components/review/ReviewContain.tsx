"use client";

import { getReviewByProduct } from "@/api/Review";
import { useEffect, useState } from "react";
import Star from "../product/Star";
import BarChart from "./BarChart";
import InfiniteReview from "./InfiniteReview";
import SpinnerComponent from "../common/SpinnerComponent";
import BarChartLoading from "./BarChartLoading";
import ReviewItem from "./ReviewItem";
import UserStore from "@/store/user";

interface props {
  itemCode: string;
  rating: number;
}
export default function ReviewContain({ itemCode, rating }: props) {
  const [reviews, setReviews] = useState<ReviewDTOonProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [reviewCount, setReviewCount] = useState<number | null>(null);
  const [ratingStats, setRatingStats] = useState<RatingStats | null>(null);
  const [myReview, setMyReview] = useState<ReviewDTOonProduct | null>(null);
  const [page, setPage] = useState(2);
  const [hasNextPage, setHasNextPage] = useState(true);
  const userStore = UserStore();
  const filterDeletedReview = (id: number) => {
    setReviews((prev) => prev.filter((review) => review.reviewId !== id));
    setReviewCount((prev) => (prev !== null ? prev - 1 : null));
    setMyReview(null);
  };

  const refreshData = async () => {
    try {
      const data = await getReviewByProduct(itemCode, page, 10);
      if (reviews.length === 0 && data.myReview) {
        setMyReview(data.myReview);
      }
      setPage((prev) => prev + 1);
      setReviews((prev) => (prev ? [...prev, ...data.reviews] : data.reviews));
      setHasNextPage(data.hasNext);
      setReviewCount(data.totalElements);
      setRatingStats(data.ratingStats);
    } catch (e) {
      setHasNextPage(false);
      alert("レビューをロード中にエラーが発生しました。");
    }
  };

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        const res = await getReviewByProduct(itemCode, 1, 10);
        if (res.myReview) {
          setMyReview(res.myReview);
        }
        setReviews(res.reviews);
        setReviewCount(res.totalElements);
        setRatingStats(res.ratingStats);
        setLoading(false);
      } catch (e: any) {}
    };
    fetchReviews();
  }, []);
  return (
    <div>
      <div className="flex ac ">
        <h2>レビュー</h2>({reviewCount}件の評価)
      </div>
      <div className="ac bar-chart-contain">
        <div className="mobie-star flex ac jc">
          <Star className="detail2-" score={rating} />
        </div>

        <div className="flex flex-col gap-05">
          {loading
            ? [5, 4, 3, 2, 1].map((num) => (
                <BarChartLoading score={num} key={num} />
              ))
            : ratingStats?.map((item, idx) => (
                <BarChart
                  score={item.score}
                  key={idx}
                  percentage={
                    reviewCount ? (item.count / reviewCount) * 100 : 0
                  }
                  num={item.count}
                />
              ))}
        </div>
      </div>
      <div className="flex-col gap-1 ">
        {loading ? (
          <div className="mt-2">
            <SpinnerComponent />
          </div>
        ) : reviews.length > 0 ? (
          <>
            {myReview && (
              <div className="mt-1">
                <h3>あなたのレビュー</h3>
                <div className="my-review mt-1 my-review-contain ">
                  <ReviewItem
                    itemCode={itemCode}
                    refreshData={filterDeletedReview}
                    review={{
                      ...myReview,
                      userNickname: userStore.userInfo
                        ? userStore.userInfo.nickname
                        : "",
                    }}
                    isMy={true}
                  />
                </div>
              </div>
            )}

            {reviews.map((review) => (
              <ReviewItem
                itemCode={itemCode}
                refreshData={filterDeletedReview}
                review={review}
                key={review.reviewId}
              />
            ))}
            <InfiniteReview
              refreshData={refreshData}
              hasNextPage={hasNextPage}
            />
          </>
        ) : (
          <div className="mt-2">レビューはまだありません。</div>
        )}
      </div>
    </div>
  );
}
