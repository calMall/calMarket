import { getProductDetail } from "@/api/Product";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import ReviewWriteContain from "@/components/review/ReviewWriteContain";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";

export default async function ReviewWrite({
  params,
}: {
  params: Promise<{ code: string }>;
}) {
  const { code } = await params;

  try {
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
        <ReviewWriteContain itemCode={code} />
      </CustomLayout>
    );
  } catch (e) {
    return <ErrorComponent />;
  }
}
