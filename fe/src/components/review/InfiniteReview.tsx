"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import InfiniteScroll from "react-infinite-scroll-component";
import ReviewItem from "./ReviewItem";
import { getReviewByProduct } from "@/api/Review";
import UserStore from "@/store/user";
import SpinnerComponent from "../common/SpinnerComponent";

interface props {
  itemCode: string;
  size: number;
  isNextPage: boolean;
}
export default function InfiniteReview({ itemCode, size, isNextPage }: props) {
  const [page, setPage] = useState(0);
  const [reviews, setReviews] = useState<ReviewDTOonProduct[]>([]);
  const [hasNextPage, setHasNextPage] = useState(isNextPage);
  const [myReview, setMyReview] = useState<ReviewDTOonProduct | null>(null);
  const [reviewCount, setReviewCount] = useState<number | null>(null);
  const userStore = UserStore();
  const refreshData = async () => {
    try {
      const data = await getReviewByProduct(itemCode, page, size);
      if (reviews.length === 0 && data.myReview) {
        setMyReview(data.myReview);
      }
      setPage((prev) => prev + 1);
      setReviews((prev) => (prev ? [...prev, ...data.reviews] : data.reviews));
      setHasNextPage(data.hasNext);
      setReviewCount(data.totalElements);
    } catch (e) {
      setHasNextPage(false);
      alert("レビューをロード中にエラーが発生しました。");
    }
  };

  const filterDeletedReview = (id: number) => {
    setReviews((prev) => prev.filter((review) => review.reviewId !== id));
    setReviewCount((prev) => (prev !== null ? prev - 1 : null));
    setMyReview(null);
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
      <InfiniteScroll
        children
        className="grid-full"
        dataLength={reviews.length}
        next={refreshData}
        hasMore={hasNextPage}
        loader={<SpinnerComponent />}
        scrollableTarget="scrollableDiv"
      />
    </>
  );
}
