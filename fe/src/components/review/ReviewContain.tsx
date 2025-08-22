"use client";

import { getReviewByProduct } from "@/api/Review";
import { useEffect, useState } from "react";
import Star from "../product/Star";
import BarChart from "./BarChart";
import InfiniteReview from "./InfiniteReview";
import SpinnerComponent from "../common/SpinnerComponent";

interface props {
  itemCode: string;
  rating: number;
}
export default function ReviewContain({ itemCode, rating }: props) {
  const [reviews, setReviews] = useState<ReviewDTOonProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [reviewCount, setReviewCount] = useState(0);
  const [ratingStats, setRatingStats] = useState<RatingStats | null>(null);
  useEffect(() => {
    const fetchReviews = async () => {
      try {
        const res = await getReviewByProduct(itemCode, 0, 10);
        setReviews(res.reviews);
        setReviewCount(res.totalElements);
        setRatingStats(res.ratingStats);
        setLoading(false);
        console.log(res);
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
          {loading ? (
            <div className="mt-2">
              <SpinnerComponent />
            </div>
          ) : (
            ratingStats?.map((item, idx) => (
              <BarChart
                score={item.score}
                key={idx}
                percentage={(item.count / reviewCount) * 100}
                num={item.count}
              />
            ))
          )}
        </div>
      </div>
      <div className="flex-col gap-1 ">
        {loading ? (
          <div className="mt-2">
            <SpinnerComponent />
          </div>
        ) : reviews.length > 0 ? (
          <InfiniteReview itemCode={itemCode} size={10} isNextPage={true} />
        ) : (
          <div className="mt-2">レビューはまだありません。</div>
        )}
      </div>
    </div>
  );
}
