"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { getReviewByUser } from "@/api/Review";
import CustomLayout from "@/components/common/CustomLayout";
import UserStore from "@/store/user";
import InfiniteReviewByUser from "@/components/review/InfiniteReviewByUser";

export default function InfiniteReview() {
  const [page, setPage] = useState(0);
  const [reviews, setReviews] = useState<ReviewDTOonProduct[]>([]);
  const [hasNextPage, setHasNextPage] = useState(true);
  const [reviewCount, setReviewCount] = useState(0);
  const userStore = UserStore();
  const refreshData = async () => {
    try {
      const data = await getReviewByUser(page, 10);
      console.log(data);
      setPage((prev) => prev + 1);
      setReviews(data.reviews);
      setHasNextPage(data.hasNext);
      setReviewCount(data.totalElements);
      console.log(data.reviews);
    } catch (e) {
      setHasNextPage(false);
      alert("エラーが発生しました。");
    }
  };
  useEffect(() => {
    refreshData();
  }, []);
  return (
    <CustomLayout>
      <h2>
        {userStore.userInfo?.nickname}様のレビュー({reviewCount})
      </h2>
      <InfiniteReviewByUser
        refreshData={refreshData}
        page={page}
        setPage={setPage}
        setReviews={setReviews}
        reviews={reviews}
        size={10}
        isNextPage={hasNextPage}
      />
    </CustomLayout>
  );
}
