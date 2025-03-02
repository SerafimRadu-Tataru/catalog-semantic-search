import json

with open("/data/products.json", "r") as f:
    products = json.load(f)
categories = sorted({product["category"] for product in products})
brands = sorted({product["brand"] for product in products})

category_map = {category: f"cat{i+1}" for i, category in enumerate(categories)}
brand_map = {brand: f"b{i+1}" for i, brand in enumerate(brands)}

with open("/data/categories.json", "w") as f:
    json.dump([{"category_id": v, "category_name": k} for k, v in category_map.items()], f, indent=4)
with open("/data/brands.json", "w") as f:
    json.dump([{"brand_id": v, "brand_name": k} for k, v in brand_map.items()], f, indent=4)

for product in products:
    product["category"] = category_map[product["category"]]
    product["brand"] = brand_map[product["brand"]]

with open("/data/products_with_ids.json", "w") as f:
    json.dump(products, f, indent=4)
