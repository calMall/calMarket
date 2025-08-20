"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import InfiniteScroll from "react-infinite-scroll-component";
import ReviewItem from "@/components/review/ReviewItem";
import { getReviewByUser } from "@/api/Review";
import CustomLayout from "@/components/common/CustomLayout";

export default function InfiniteReview() {
  const [page, setPage] = useState(0);
  const [reviews, setReviews] = useState<ReviewDTOonProduct[]>([]);
  const [hasNextPage, setHasNextPage] = useState(true);
  const refreshData = async () => {
    try {
      const data = await getReviewByUser(page, 10);
      console.log(data);
      setPage((prev) => prev + 1);
      setReviews((prev) => (prev ? [...prev, ...data.reviews] : data.reviews));
      setHasNextPage(data.hasNext);
    } catch (e) {
      setHasNextPage(false);
      alert("エラーが発生しました。");
    }
  };
  useEffect(() => {
    async () => {
      await refreshData();
    };
  }, []);
  return (
    <CustomLayout>
      {reviews.map((review) => (
        <ReviewItem review={review} key={review.reviewId} />
      ))}
      <InfiniteScroll
        children
        className="grid-full"
        dataLength={reviews.length}
        next={refreshData}
        hasMore={hasNextPage}
        loader={
          <div className="flex jc mt-1 mb-1 wf">
            <Image width={50} height={50} src="/spinner.gif" alt="loading" />
          </div>
        }
        scrollableTarget="scrollableDiv"
      />
    </CustomLayout>
  );
}
