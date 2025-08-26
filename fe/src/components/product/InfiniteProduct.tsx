"use client";

import { rakutenSearch } from "@/api/Rakuten";
import Image from "next/image";
import { useEffect, useState } from "react";
import InfiniteScroll from "react-infinite-scroll-component";
import ProductItem from "./ProductItem";
import SpinnerComponent from "../common/SpinnerComponent";

interface props {
  keyword: string;
  hits: number;
  isNextPage: boolean;
}
export default function InfiniteProduct({ keyword, hits, isNextPage }: props) {
  const [page, setPage] = useState(2);
  const [products, setProducts] = useState<rakutenApiItem[]>([]);
  const [hasNextPage, setHasNextPage] = useState(isNextPage);
  const refreshData = async () => {
    try {
      const data = await rakutenSearch(keyword, page, hits);
      setPage((prev) => prev + 1);
      setProducts((prev) => (prev ? [...prev, ...data.Items] : data.Items));
      setHasNextPage(data.last - data.page > 0);
    } catch (e) {
      setHasNextPage(false);
      alert(
        "リクエストが集中しているため、しばらくしてからもう一度お試しください。"
      );
    }
  };
  useEffect(() => {
    setPage(2);
    setProducts([]);
    setHasNextPage(isNextPage);
    async () => {
      await refreshData();
    };
  }, [keyword]);
  return (
    <>
      <div className="search-list-contain mt-1">
        {products.map((product, idx) => (
          <ProductItem
            product={product}
            key={String(idx) + product.Item.itemCode}
          />
        ))}
      </div>
      <InfiniteScroll
        children
        className="grid-full"
        dataLength={products.length}
        next={refreshData}
        hasMore={hasNextPage}
        loader={
          <div className="mt-1">
            <SpinnerComponent />
          </div>
        }
        scrollableTarget="scrollableDiv"
      />
    </>
  );
}
