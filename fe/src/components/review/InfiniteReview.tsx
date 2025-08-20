"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import InfiniteScroll from "react-infinite-scroll-component";
import ReviewItem from "./ReviewItem";
import { getReviewByProduct } from "@/api/Review";

interface props {
  itemCode: string;
  size: number;
  isNextPage: boolean;
}
export default function InfiniteReview({ itemCode, size, isNextPage }: props) {
  const [page, setPage] = useState(0);
  const [reviews, setReviews] = useState<ReviewDTOonProduct[]>([]);
  const [hasNextPage, setHasNextPage] = useState(isNextPage);
  const refreshData = async () => {
    try {
      const data = await getReviewByProduct(itemCode, page, size);
      setPage((prev) => prev + 1);
      setReviews((prev) => (prev ? [...prev, ...data.reviews] : data.reviews));
      setHasNextPage(data.hasNext);
    } catch (e) {
      setHasNextPage(false);
      alert(
        "リクエストが集中しているため、しばらくしてからもう一度お試しください。"
      );
    }
  };
  useEffect(() => {
    setPage(0);
    setReviews([]);
    setHasNextPage(isNextPage);
    async () => {
      await refreshData();
    };
  }, [itemCode]);
  return (
    <>
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
    </>
  );
}
