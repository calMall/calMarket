"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import InfiniteScroll from "react-infinite-scroll-component";
import ReviewItem from "./ReviewItem";
import { getReviewByProduct } from "@/api/Review";
import UserStore from "@/store/user";
import SpinnerComponent from "../common/SpinnerComponent";

interface props {
  hasNextPage: boolean;
  refreshData: () => Promise<void>;
}
export default function InfiniteReview({ hasNextPage, refreshData }: props) {
  return (
    <>
      <InfiniteScroll
        children
        className="grid-full"
        dataLength={10}
        next={refreshData}
        hasMore={hasNextPage}
        loader={<SpinnerComponent />}
        scrollableTarget="scrollableDiv"
      />
    </>
  );
}
