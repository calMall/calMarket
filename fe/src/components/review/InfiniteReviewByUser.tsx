"use client";

import Image from "next/image";
import InfiniteScroll from "react-infinite-scroll-component";
import ReviewItem from "./ReviewItem";
import SpinnerComponent from "../common/SpinnerComponent";

interface props {
  size: number;
  isNextPage: boolean;
  reviews: ReviewDTOonProduct[];
  setReviews: React.Dispatch<React.SetStateAction<ReviewDTOonProduct[]>>;
  page: number;
  setPage: React.Dispatch<React.SetStateAction<number>>;
  refreshData: () => Promise<void>;
}
export default function InfiniteReviewByUser({
  isNextPage,
  reviews,
  setReviews,
  refreshData,
}: props) {
  const filterDeletedReview = (id: number) => {
    setReviews((prev) => prev.filter((review) => review.reviewId !== id));
  };

  return (
    <>
      {reviews.map((review) => (
        <ReviewItem
          itemCode={review.itemCode}
          refreshData={filterDeletedReview}
          review={review}
          key={review.reviewId}
        />
      ))}
      <InfiniteScroll
        children
        className="grid-full"
        dataLength={reviews.length}
        next={refreshData}
        hasMore={isNextPage}
        loader={<SpinnerComponent />}
        scrollableTarget="scrollableDiv"
      />
    </>
  );
}
