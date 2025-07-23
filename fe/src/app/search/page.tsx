import { rakutenSearch } from "@/api/Rakuten";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import InfiniteProduct from "@/components/product/InfiniteProduct";
import ProductItem from "@/components/product/ProductItem";

interface props {
  searchParams: Promise<{ q: string }>;
}

export default async function ProductsSearch({ searchParams }: props) {
  const { q } = await searchParams;
  try {
    const data = await rakutenSearch(q, 1, 20);
    return (
      <CustomLayout>
        <h1 className="flex ac jc">「{q}」の検索結果</h1>
        {data.count > 0 ? (
          <div className="search-list-contain mt-2">
            {data.Items.map((item) => (
              <ProductItem
                imageSize={512}
                product={item}
                key={item.Item.itemCode}
              />
            ))}
            <InfiniteProduct
              keyword={q}
              isNextPage={data.last - data.page > 0}
              hits={20}
            />
          </div>
        ) : (
          <h3>検索結果がありません。</h3>
        )}
      </CustomLayout>
    );
  } catch (e) {
    return <ErrorComponent />;
  }
}
