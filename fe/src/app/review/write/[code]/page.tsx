import { getProductDetail } from "@/api/Product";
import { getReviewDetail } from "@/api/Review";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import ReviewWriteContain from "@/components/review/ReviewWriteContain";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";

export default async function ReviewWrite({
  params,
  searchParams,
}: {
  params: Promise<{ code: string }>;
  searchParams: Promise<{ reviewId: string }>;
}) {
  try {
    const { code } = await params;
    const { reviewId } = await searchParams;
    console.log(decodeURIComponent(code), reviewId);
    let initialData: ReviewDTOonProduct | null = null;
    if (reviewId !== undefined) {
      const data = await getReviewDetail(Number(reviewId));
      initialData = data;
      console.log(data);
    }
    const { product } = await getProductDetail(code);
    return (
      <CustomLayout>
        <h2>商品はいかがでしたか？</h2>
        <Link
          href={`/product/${code}`}
          className="flex bb simple-review-contain"
        >
          <div className="rt simple-order-img simple-img">
            <ContainImage
              alt="poduct"
              url={newImageSizing(product.imageUrls[0], 512)}
            />
          </div>
          <div className="review-itemname">{product.itemName}</div>
        </Link>
        <ReviewWriteContain
<<<<<<< HEAD
          reviewId={Number(reviewId)}
=======
>>>>>>> cab78baf0bacc370f5c5c294241594d6449714ca
          initialData={initialData ? initialData : null}
          itemCode={code}
        />
      </CustomLayout>
    );
  } catch (e) {
    return <ErrorComponent />;
  }
}
