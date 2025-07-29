"use client";

import { getReviewByProduct } from "@/api/Review";
import { useEffect, useState } from "react";
import Star from "../product/Star";
import BarChart from "./BarChart";
import Image from "next/image";
import ReviewItem from "./ReviewItem";

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
        const res = await getReviewByProduct(itemCode);
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
          {loading ? (
            <div>
              <div className="loading-spinner flex ac">
                <Image
                  src={"/spinner.gif"}
                  alt="loading"
                  width={50}
                  height={50}
                ></Image>
              </div>
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
      <div className="flex-col gap-1 mt-2 ">
        {loading ? (
          <div>
            <div className="loading-spinner flex ac jc">
              <Image
                src={"/spinner.gif"}
                alt="loading"
                width={50}
                height={50}
              ></Image>
            </div>
          </div>
        ) : reviews.length > 0 ? (
          reviews.map((review) => (
            <ReviewItem key={review.reviewId} review={review} />
          ))
        ) : (
          <div>レビューはまだありません。</div>
        )}
      </div>
    </div>
  );
}
